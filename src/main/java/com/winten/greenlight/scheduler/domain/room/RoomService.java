package com.winten.greenlight.scheduler.domain.room;

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

    private long calculateMetricCounterBucket() {
        long currentBucketStart = (System.currentTimeMillis() / 3000) * 3000;
        return currentBucketStart - 3000;
    }

    public void relocateCustomers() {
        // 로직
        List<Room> rooms = roomRepository.getAllRoomList();

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
            roomRepository.increaseMetricCount(room.getRoomId(), WaitStatus.ENTERED, targetBucket, movedCount);
            log.debug("{}명 입장 완료", movedCount);
        }
    }

    public void removeExpiredEnteredQueue(Integer durationSeconds) {
        if (durationSeconds <= 0L) {
            throw new IllegalArgumentException("expireMinute must be positive.");
        }

        // 현재 시각 기준 N초 이전(ex: 1초 = 1000밀리초) 이전 timestamp 계산
        long currentTimeMillis = System.currentTimeMillis();
        long expireTime = currentTimeMillis - (durationSeconds * 1000L);

        List<Room> rooms = roomRepository.getAllRoomList();
        for (Room room: rooms) {
            roomRepository.removeEnteredQueueRanged(room.getRoomId(), expireTime);
        }
    }

    // 3초에 한번 돌리는걸 가정
    public void recordRoomMetric3s() {
        long now = System.currentTimeMillis();
        // 1. Metric 측정 기준 시작시간. 3초 단위로 딱 떨어지도록 계산
        long currentBucketStart = (now / 3000) * 3000 - 3000;
        // 대시보드에는 직전에 완성된 3초 버킷 데이터를 제공
        long targetBucket = currentBucketStart - 3000;
        // 이 시간 이전의 대기/활성 사용자수 측정 (totalWaiting, totalActive)
        long countThreshold = currentBucketStart + 2999;
        // 2. 만료 기준 시간 (현재 시간 - 60초(60000ms))
        long deadThreshold = now - 60000;

        var rooms = roomRepository.getAllRoomList();
        var updated = false;
        for (var room: rooms) {
            if (!room.getEnabled()) {
                continue;
            }
            var metric = roomRepository.calculateRoomMetric(
                    room.getRoomId(),
                    targetBucket,
                    countThreshold,
                    deadThreshold
            );
            roomRepository.saveRoomMetric(metric);
            updated = true;
        }
        if (updated) {
            roomRepository.updateRoomMetricVersion(currentBucketStart); // 버전 업데이트
        }
    }
}