package com.winten.greenlight.scheduler.domain.room;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.winten.greenlight.scheduler.db.repository.redis.room.CachedRoomService;
import com.winten.greenlight.scheduler.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import com.winten.greenlight.scheduler.domain.influx.InfluxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final CachedRoomService cachedRoomService;
    private final InfluxService influxService;

    @Value("${influxdb.org}")
    private String influxOrganization;
    @Value("${influxdb.bucket}")
    private String influxBucket;

    private final String MEASUREMENT_ROOM_METRIC = "room_metric";
    private static final int EXPIRED_WAITING_CHUNK_SIZE = 1_000;
    private static final int EXPIRED_WAITING_MAX_CHUNKS = 5;
    /**
     * 승격 Lua 한 방이 메인 스레드를 오래 잡지 않도록 하는 상한.
     * 1초 입장량은 maxTrafficPerSecond가 정하고, EVAL 크기는 이 값이 정한다.
     * Lua unpack 한도(~8000) 안에서 ZADD 인자 1+2N 이 들어가도록 1000으로 둔다.
     */
    static final int MOVE_TICKET_CHUNK_SIZE = 1_000;

    static long calculateMetricCounterBucket(long now) {
        long currentBucketStart = (now / 3000) * 3000;
        return currentBucketStart - 3000;
    }

    static long calculateMetricCollectionBucket(long now) {
        return calculateMetricCounterBucket(now) - 3000;
    }

    public void relocateCustomers() {
        // 로직
        List<Room> rooms = cachedRoomService.getAllRoomList();

        for (Room room: rooms) {
            if (!room.getEnabled() || room.getCapacity() == 0 || room.getMaxTrafficPerSecond() == 0) {
                continue; // Room이 비활성화 되어있다면 pass
            }

            // 현재 입장완료한 고객 수 count. ENTERED size = 화면에 머물러있는 고객 수
            long enteredCount = roomRepository.countEnteredCustomersByRoomId(room.getRoomId());

            // capacity - 화면에 머물러있는 고객 수 = 입장 가능한 고객 수
            long remainingCapacity = Math.max(room.getCapacity() - enteredCount, 0);

            // 다음으로 입장할 고객 수 (한번에 입장하는 고객 수는 maxTrafficPerSecond 보다 커질 수 없음.
            long nextCount = Math.min(remainingCapacity, room.getMaxTrafficPerSecond());

            if (nextCount <= 0) {
                continue; // 입장 불가능한 상태라면 스킵 (room 포화상태)
            }

            long movedCount = 0;
            long remaining = nextCount;
            while (remaining > 0) {
                long chunk = Math.min(remaining, MOVE_TICKET_CHUNK_SIZE);
                Long moved = roomRepository.moveTicketsToEntered(room.getRoomId(), chunk);
                if (moved == null || moved <= 0) {
                    break;
                }
                movedCount += moved;
                remaining -= moved;
                if (moved < chunk) {
                    break;
                }
            }
            long targetBucket = calculateMetricCounterBucket(System.currentTimeMillis());
            roomRepository.increaseMetricCountBy(room.getRoomId(), WaitStatus.ENTERED, targetBucket, movedCount);
        }
    }

    public void removeExpired() {
        long now = System.currentTimeMillis();

        long deadHeartbeatThreshold = now - 60000; // 2. 만료 기준 시간 (현재 시간 - 60초(60000ms))
        long metricBucket = calculateMetricCounterBucket(now);
        long enteredQueueExpireTime = now - (86400_000L); // 1일

        var rooms = cachedRoomService.getAllRoomList();

        var waitingHeartbeatThreshold = now - 60_000;
        for (var room: rooms) {
            long deadHeartbeatCount = roomRepository.removeAndCountDeadEnteredHeartbeat(room.getRoomId(), deadHeartbeatThreshold);
            roomRepository.increaseMetricCountBy(room.getRoomId(), WaitStatus.EXITED, metricBucket, deadHeartbeatCount);
            roomRepository.removeEnteredQueue(room.getRoomId(), enteredQueueExpireTime); // 1일 지난 queue:ENTERED 삭제

            for (int i = 0; i < EXPIRED_WAITING_MAX_CHUNKS; i++) {
                List<String> expiredTicketList = roomRepository.getAndRemoveExpiredWaitingHeartbeat(
                        room.getRoomId(),
                        waitingHeartbeatThreshold,
                        EXPIRED_WAITING_CHUNK_SIZE
                );
                if (expiredTicketList == null || expiredTicketList.isEmpty()) {
                    break;
                }
                roomRepository.removeQueueBulk(room.getRoomId(), WaitStatus.WAITING, expiredTicketList);
                roomRepository.increaseMetricCountBy(room.getRoomId(), WaitStatus.CANCELLED, metricBucket, expiredTicketList.size());
                if (expiredTicketList.size() < EXPIRED_WAITING_CHUNK_SIZE) {
                    break;
                }
            }
        }
    }

    // 3초에 한번 돌리는걸 가정
    public void recordRoomMetric3s() {
        long now = System.currentTimeMillis();
        long completedBucketStart = calculateMetricCounterBucket(now);
        long targetBucket = calculateMetricCollectionBucket(now); // 대시보드에는 직전에 완성된 3초 버킷 데이터를 제공

        var rooms = cachedRoomService.getAllRoomList();
        var updated = false;
        var metricPoints = new ArrayList<Point>();
        for (var room: rooms) {
            if (!room.getEnabled()) { // 비활성화인 경우 기록하지 않음
                continue;
            }
            var metric = roomRepository.calculateRoomMetric(
                    room.getRoomId(),
                    targetBucket
            );
            metric.setRoomCapacity(room.getCapacity());
            long estimatedWaitTime = calculateEstimatedWaitTime(room.getCapacity(), metric.getTotalActive(), metric.getTotalWaiting(), metric.getRecentlyExited());
            metric.setEstimatedWaitTime(estimatedWaitTime);
            roomRepository.saveRoomMetricLatest(metric);
            updated = true;
            // 운영환경인 경우에만 influxDB에 저장
            if (room.getRoomEnvironment() == RoomEnvironment.LIVE) {
                var metricPoint = makeMetricPoint(room, metric);
                metricPoints.add(metricPoint);
            }
        }
        if (updated) {
            roomRepository.updateRoomMetricVersion(completedBucketStart); // 버전 업데이트
        }

        influxService.writePointsAsync(influxBucket, influxOrganization, metricPoints);
    }

    private long calculateEstimatedWaitTime(long capacity, long totalActive, long totalWaiting, long recentlyExited) {
        if (capacity <= 0) { // capacity가 0보다 작으면 입장불가
            return -1;
        }
        long remainder = totalWaiting - (capacity - totalActive);
        if (remainder <= 0) {  // 1. 남는 자리가 있다면 바로입장 가능.
            return 0;
        }
        if (recentlyExited < capacity * 3) { // 2. 집계된 recentlyExited가 너무 적을 때 임의로 수치를 조정함
            // recentlyExited는 3분간 나간 전체 사용자 수. 30초 머무는 상황이므로 capacity가 1일 때 3분동안 6명이 나감.
            recentlyExited = Math.round(capacity * 2.1 + recentlyExited * 0.3);
        }
        return (remainder * 180) / recentlyExited;
    }

    private Point makeMetricPoint(Room r, RoomMetric m) {
        return Point.measurement(MEASUREMENT_ROOM_METRIC)
                .addTag("room_id", m.getRoomId())
                .addTag("room_environment", r.getRoomEnvironment().name())
                .addField("room_capacity", m.getRoomCapacity())
                .addField("total_waiting", m.getTotalWaiting())
                .addField("total_active", m.getTotalActive())
                .addField("recently_exited_3m", m.getRecentlyExited())
                .addField("waiting_count", m.getWaitingCount())
                .addField("entered_count", m.getEnteredCount())
                .addField("exited_count", m.getExitedCount())
                .addField("cancelled_count", m.getCancelledCount())
                .addField("estimated_wait_time", m.getEstimatedWaitTime())
                .time(Instant.now(), WritePrecision.MS);
    }
}
