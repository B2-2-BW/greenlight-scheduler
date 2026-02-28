package com.winten.greenlight.scheduler.db.repository.redis.room;

import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import com.winten.greenlight.scheduler.domain.room.RoomMetric;
import com.winten.greenlight.scheduler.domain.room.Room;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoomRepository {
    private final RedisKeyBuilder redisKeyBuilder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;

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
        var redisScript = getMoveTicketRedisScript();

        // Score는 String으로 전달해도 Redis 내부에서 double로 파싱됨
        String nowScore = String.valueOf(System.currentTimeMillis());

        return redisTemplate.execute(
                redisScript,
                List.of(waitingKey, enteredKey, enteredHeartbeatKey), // KEYS[1], KEYS[2], KEYS[3]
                String.valueOf(count),           // ARGV[1]
                nowScore                         // ARGV[2]
        );
    }

    private @NonNull DefaultRedisScript<Long> getMoveTicketRedisScript() {
        String script = """
local members = redis.call('ZRANGE', KEYS[1], 0, ARGV[1] - 1)
if #members > 0 then
    redis.call('ZREM', KEYS[1], unpack(members))
    for _, member in ipairs(members) do
        redis.call('ZADD', KEYS[2], ARGV[2], member)
        redis.call('ZADD', KEYS[3], ARGV[2], member)
    end
    return #members
else
    return 0
end
""";
        var redisScript = new DefaultRedisScript<Long>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    public void removeEnteredQueueRanged(String roomId, long expireTime) {
        var key = redisKeyBuilder.roomQueue(roomId, WaitStatus.ENTERED);
        Long count = redisTemplate.opsForZSet().removeRangeByScore(key, 0, expireTime);
        if (count != null && count > 0) {
            log.info("[removeEnteredQueueRanged] RoomId: {}, {}명 삭제.", roomId, count);
        }
    }

    private static @lombok.NonNull DefaultRedisScript<List> getRoomMetricCalculationScript() {
        String script = """
            local countThreshold = tonumber(ARGV[1])
            local deadThreshold = tonumber(ARGV[2])
    
            -- 1. 대기(전체): countThreshold 이전의 데이터만 ZCOUNT로 계산
            local totalWaiting = redis.call('ZCOUNT', KEYS[1], '-inf', countThreshold)
    
            -- 2. 이탈(만료): deadThreshold 이전 데이터 삭제 및 삭제된 개수 반환
            local deadCount = redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', deadThreshold)
    
            -- 3. 활성(전체): countThreshold 이전의 데이터만 ZCOUNT로 계산
            local totalActive = redis.call('ZCOUNT', KEYS[2], '-inf', countThreshold)
    
            -- 4. 3초 구간 유입/입장/이탈 데이터 조회
            local waitingIncr = tonumber(redis.call('GET', KEYS[3]) or '0')
            local enteredIncr = tonumber(redis.call('GET', KEYS[4]) or '0')
            local exitedIncr = tonumber(redis.call('GET', KEYS[5]) or '0')
    
            return { totalWaiting, totalActive, waitingIncr, enteredIncr, exitedIncr, deadCount }
    """;

        // 반환 타입을 List<Long>으로 지정
        var redisScript = new DefaultRedisScript<List>();
        redisScript.setScriptText(script);
        redisScript.setResultType(List.class);
        return redisScript;
    }

    /**
     *
     * 대시보드 메트릭 집계 및 만료 활성사용자 삭제처리
     *  <ul>
     *   <li>대기(전체): totalWaiting (queue:WAITING) -> 대기열 Sorted Set: 실시간으로 기록됨</li>
     *   <li>활성(전체): totalActive (heartbeat:ENTERED) -> 화면을 보고있는 활성사용자 수 Sorted Set: 실시간으로 기록됨 (60초 지난건 삭제)</li>
     *   <li>대기(3초): waitingIncr (metric:counter:WAITING:{timestamp})-> 사용자 유입량 counter: 실시간으로 기록됨. 3초 Time-Bucketed Key 사용</li>
     *   <li>입장(3초): enteredIncr (metric:counter:ENTERED:{timestamp}) -> 사용자 입장량 counter: 실시간으로 기록됨. 3초 Time-Bucketed Key 사용</li>
     *   <li>이탈(3초): exitedIncr + dead
     *    <ul>
     *     <li>이탈(실시간): exitedIncr (metric:counter:EXITED:{timestamp} -> 이탈량 counter: 실시간으로 기록됨. 3초 Time-Bucketed Key 사용</p></li>
     *     <li>이탈(만료): dead (heartbeat:ENTERED) -> 활성사용자 수 sorted set. 3초마다 60초 지난 heartbeat 체크 후 삭제)</li>
     *    </ul>
     *   </li>
     * </ul>
     * @param roomId 대기열 ID
     * @param targetBucket 3초동안 대기, 입장, 이탈량이 기록된 Time bucket id
     * @param countThreshold 이 시간 이전의 대기/활성 사용자수 측정 (totalWaiting, totalActive)
     * @param deadThreshold 이 시간이 지난 활성사용자는 삭제 (totalActive)
     */
    public RoomMetric calculateRoomMetric(String roomId, long targetBucket, long countThreshold, long deadThreshold) {
        // Time-Bucketed Key에 타임스탬프 조합 (예: room:1:metric:counter:WAITING:1700000002000)
        List<String> keys = List.of(
            redisKeyBuilder.roomQueue(roomId, WaitStatus.WAITING),
            redisKeyBuilder.roomHeartbeat(roomId, WaitStatus.ENTERED),
            redisKeyBuilder.roomMetricCounter(roomId, WaitStatus.WAITING, targetBucket),
            redisKeyBuilder.roomMetricCounter(roomId, WaitStatus.ENTERED, targetBucket),
            redisKeyBuilder.roomMetricCounter(roomId, WaitStatus.EXITED, targetBucket)
        );

        // script, keys, 그리고 ARGV에 들어갈 deadThreshold 전달
        @SuppressWarnings("unchecked")
        List<Long> result = (List<Long>) redisTemplate.execute(
            getRoomMetricCalculationScript(),
            keys,
            String.valueOf(countThreshold),
            String.valueOf(deadThreshold)
        );

        var metric = RoomMetric.builder()
                .roomId(roomId)
                .totalWaiting(result.get(0))
                .totalActive(result.get(1))
                .waitingCount(result.get(2))
                .enteredCount(result.get(3))
                .exitedCount(result.get(4) + result.get(5)) // 이탈은 exited + dead 합한 값
                .build();

        long waitingRate = metric.getWaitingCount();
        long enteredRate = metric.getEnteredCount();
        long exitedRate = metric.getExitedCount();
        var estimatedWaitTime = enteredRate != 0
                ? metric.getTotalActive() / enteredRate
                : 0 ;
        metric.setWaitingRate(waitingRate);
        metric.setEnteredRate(enteredRate);
        metric.setExitedRate(exitedRate);
        metric.setEstimatedWaitTime(estimatedWaitTime);
        return metric;
    }

    public void saveRoomMetric(RoomMetric metric) {
        String key = redisKeyBuilder.roomMetricLatest(metric.getRoomId());
        String value = jsonMapper.writeValueAsString(metric);
        redisTemplate.opsForValue().set(key, value);
    }

    public void updateRoomMetricVersion(long version) {
        String key = redisKeyBuilder.roomMetricVersion();
        redisTemplate.opsForValue().set(key, String.valueOf(version));
    }

    public void increaseMetricCount(String roomId, WaitStatus metricType, long targetBucket, long count) {
        if (count <= 0) {
            return;
        }
        String key = redisKeyBuilder.roomMetricCounter(roomId, metricType, targetBucket);
        long updated = redisTemplate.opsForValue().increment(key, count);
        if (updated == 1L) { // count == 1 이면 처음 만들어진 키이므로 TTL 세팅
            redisTemplate.expire(key, Duration.ofSeconds(30));
        }
    }
}