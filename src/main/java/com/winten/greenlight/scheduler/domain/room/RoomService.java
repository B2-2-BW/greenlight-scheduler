package com.winten.greenlight.scheduler.domain.room;

import com.winten.greenlight.scheduler.db.repository.redis.room.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;

    public void processQueue() {
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

            log.debug("room {} | capacity: {} | 입장 가능한 인원: {}", room.getName(), room.getCapacity(), nextCount);

            if (nextCount <= 0) {
                continue; // 입장 불가능한 상태라면 스킵 (room 포화상태)
            }

            // ticket 추출
            long movedCount = roomRepository.moveTicketsToEntered(room.getRoomId(), nextCount);
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

}