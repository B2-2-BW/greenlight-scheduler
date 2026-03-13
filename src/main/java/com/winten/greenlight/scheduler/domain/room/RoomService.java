package com.winten.greenlight.scheduler.domain.room;

import com.winten.greenlight.scheduler.db.repository.redis.room.CachedRoomService;
import com.winten.greenlight.scheduler.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final CachedRoomService cachedRoomService;

    private long calculateMetricCounterBucket() {
        long currentBucketStart = (System.currentTimeMillis() / 3000) * 3000;
        return currentBucketStart - 3000;
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

            // ticket 추출
            long movedCount = roomRepository.moveTicketsToEntered(room.getRoomId(), nextCount);
            long targetBucket = this.calculateMetricCounterBucket();
            roomRepository.increaseMetricCountBy(room.getRoomId(), WaitStatus.ENTERED, targetBucket, movedCount);
        }
    }

    public void removeExpired() {
        long now = System.currentTimeMillis();



        long deadHeartbeatThreshold = now - 60000; // 2. 만료 기준 시간 (현재 시간 - 60초(60000ms))
        long metricBucket = (now / 3000) * 3000; // metric counter bucket

        var rooms = cachedRoomService.getAllRoomList();
        for (var room: rooms) {
            long deadHeartbeatCount = roomRepository.removeAndCountDeadEnteredHeartbeat(room.getRoomId(), deadHeartbeatThreshold);
            roomRepository.increaseMetricCountBy(room.getRoomId(), WaitStatus.EXITED, metricBucket, deadHeartbeatCount);
        }
    }

    // 3초에 한번 돌리는걸 가정
    public void recordRoomMetric3s() {
        long now = System.currentTimeMillis();
        long currentBucketStart = (now / 3000) * 3000; // 1. Metric 측정 기준 시작시간. 3초 단위로 딱 떨어지도록 계산
        long targetBucket = currentBucketStart - 3000; // 대시보드에는 직전에 완성된 3초 버킷 데이터를 제공
        long countThreshold = currentBucketStart + 2999; // 이 시간 이전의 대기/활성 사용자수 측정 (totalWaiting, totalActive)

        var rooms = cachedRoomService.getAllRoomList();
        var updated = false;
        for (var room: rooms) {
            if (!room.getEnabled()) {
                continue;
            }
            var metric = roomRepository.calculateRoomMetric(
                    room.getRoomId(),
                    targetBucket,
                    countThreshold
            );
            long estimatedWaitTime = calculateEstimatedWaitTime(room, metric);
            metric.setEstimatedWaitTime(estimatedWaitTime);
            roomRepository.saveRoomMetricLatest(metric);
            updated = true;
        }
        if (updated) {
            roomRepository.updateRoomMetricVersion(currentBucketStart); // 버전 업데이트
        }
    }

    private long calculateEstimatedWaitTime(Room room, RoomMetric metric) {
        long capacity = room.getCapacity();
        long current = metric.getTotalActive();

    }
}