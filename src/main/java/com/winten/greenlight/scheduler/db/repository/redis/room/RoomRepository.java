package com.winten.greenlight.scheduler.db.repository.redis.room;

import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import com.winten.greenlight.scheduler.domain.room.RoomMetric;
import com.winten.greenlight.scheduler.domain.room.Room;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoomRepository {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;
    private final RoomRedisScript roomRedisScript;

    public List<Room> getAllRoomList() {
        String pattern = redisKeyBuilder.allRoomMeta();
        List<Room> rooms = new ArrayList<>();

        try (var cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build())) {

            while (cursor.hasNext()) {
                String key = cursor.next();
                String roomMetaJson = redisTemplate.opsForValue().get(key);
                if (roomMetaJson != null) {
                    try {
                        Room room = jsonMapper.readValue(roomMetaJson, Room.class);
                        rooms.add(room);
                    } catch (JacksonException e) {
                        log.error("Failed to parse room meta: {}", key, e);
                    }
                }
            }
        }

        return rooms;
    }

    public long countEnteredCustomersByRoomId(String roomId) {
        var key = redisKeyBuilder.roomHeartbeat(roomId, WaitStatus.ENTERED);
        Long count = redisTemplate.opsForZSet().size(key);
        return count != null ? count : 0L;
    }

    public Long moveTicketsToEntered(String roomId, long count) {
        String waitingKey = redisKeyBuilder.roomQueue(roomId, WaitStatus.WAITING);
        String enteredKey = redisKeyBuilder.roomQueue(roomId, WaitStatus.ENTERED);
        String enteredHeartbeatKey = redisKeyBuilder.roomHeartbeat(roomId, WaitStatus.ENTERED);

        // Lua Script: 순서 보장 없이 동일한 Timestamp로 Bulk Insert
        var redisScript = roomRedisScript.getMoveTicketRedisScript();

        // Score는 String으로 전달해도 Redis 내부에서 double로 파싱됨
        String nowScore = String.valueOf(System.currentTimeMillis());

        return redisTemplate.execute(
                redisScript,
                List.of(waitingKey, enteredKey, enteredHeartbeatKey), // KEYS[1], KEYS[2], KEYS[3]
                String.valueOf(count),           // ARGV[1]
                nowScore                         // ARGV[2]
        );
    }



    /**
     *
     * 대시보드 메트릭 집계 및 만료 활성사용자 삭제처리
     *  <ul>
     *   <li>대기(전체): totalWaiting (queue:WAITING) -> 대기열 Sorted Set: 실시간으로 기록됨</li>
     *   <li>활성(전체): totalActive (heartbeat:ENTERED) -> 화면을 보고있는 활성사용자 수 Sorted Set: 실시간으로 기록됨 (60초 지난건 삭제)</li>
     *   <li>대기(3초): waitingIncr (metric:counter:WAITING:{timestamp})-> 사용자 유입량 counter: 실시간으로 기록됨. 3초 Time-Bucketed Key 사용</li>
     *   <li>입장(3초): enteredIncr (metric:counter:ENTERED:{timestamp}) -> 사용자 입장량 counter: 실시간으로 기록됨. 3초 Time-Bucketed Key 사용</li>
     *   <li>이탈(3초): exitedIncr (metric:counter:EXITED:{timestamp} -> 이탈량 counter: 실시간으로 기록됨. 3초 Time-Bucketed Key 사용</p></li>
     * </ul>
     * @param roomId 대기열 ID
     * @param targetBucket 3초동안 대기, 입장, 이탈량이 기록된 Time bucket id
     * @param countThreshold 이 시간 이전의 대기/활성 사용자수 측정 (totalWaiting, totalActive)
     */
    public RoomMetric calculateRoomMetric(
            String roomId,
            long targetBucket,
            long countThreshold
    ) {
        List<String> keys = new ArrayList<>();
        // 1~5번 키: 대기, 활성, 증분 데이터 키
        keys.add(redisKeyBuilder.roomQueue(roomId, WaitStatus.WAITING));
        keys.add(redisKeyBuilder.roomHeartbeat(roomId, WaitStatus.ENTERED));
        keys.add(redisKeyBuilder.roomMetricCounter(roomId, WaitStatus.WAITING, targetBucket));
        keys.add(redisKeyBuilder.roomMetricCounter(roomId, WaitStatus.ENTERED, targetBucket));
        keys.add(redisKeyBuilder.roomMetricCounter(roomId, WaitStatus.EXITED, targetBucket));
        // 6~65번 키: 과거 3분(60개) 동안의 EXITED 버킷 키
        for (int i = 0; i < 60; i++) {
            long pastBucket = targetBucket - (i * 3000);
            keys.add(redisKeyBuilder.roomMetricCounter(roomId, WaitStatus.EXITED, pastBucket));
        }

        @SuppressWarnings("unchecked")
        List<Long> result = (List<Long>) redisTemplate.execute(
            roomRedisScript.getRoomMetricCalculationScript(),
            keys,
            String.valueOf(countThreshold)
        );

        var metric = RoomMetric.builder()
                .roomId(roomId)
                .totalWaiting(result.get(0))
                .totalActive(result.get(1))
                .recentlyExited(result.get(2))
                .waitingCount(result.get(3))
                .enteredCount(result.get(4))
                .exitedCount(result.get(5)) // 이탈은 exited + dead 합한 값
                .build();

        double waitingRate = (double) metric.getWaitingCount() / 3.0;
        double enteredRate = (double) metric.getEnteredCount() / 3.0;
        double exitedRate = (double) metric.getExitedCount() / 3.0;
        metric.setWaitingRate(waitingRate);
        metric.setEnteredRate(enteredRate);
        metric.setExitedRate(exitedRate);
        return metric;
    }

    public void saveRoomMetricLatest(RoomMetric metric) {
        String key = redisKeyBuilder.roomMetricLatest(metric.getRoomId());
        String value = jsonMapper.writeValueAsString(metric);
        redisTemplate.opsForValue().set(key, value);
    }

    public void updateRoomMetricVersion(long version) {
        String key = redisKeyBuilder.roomMetricVersion();
        redisTemplate.opsForValue().set(key, String.valueOf(version));
    }


    public void increaseMetricCountBy(String roomId, WaitStatus metricType, long targetBucket, long count) {
        if (count <= 0) {
            return;
        }
        var key = redisKeyBuilder.roomMetricCounter(roomId, metricType, targetBucket);
        redisTemplate.execute(
                roomRedisScript.getIncreaseMetricCountByRedisScript(),
                List.of(key),
                String.valueOf(count),
                "180"
        );
    }

    public Long removeAndCountDeadEnteredHeartbeat(String roomId, long deadHeartbeatThreshold) {
        var key = redisKeyBuilder.roomHeartbeat(roomId, WaitStatus.ENTERED);
        return redisTemplate.opsForZSet().removeRangeByScore(key, 0, deadHeartbeatThreshold);
    }
}