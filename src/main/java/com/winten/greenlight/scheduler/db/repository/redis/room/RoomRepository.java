package com.winten.greenlight.scheduler.db.repository.redis.room;

import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import com.winten.greenlight.scheduler.domain.room.Room;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

    private @NonNull DefaultRedisScript<Long> getRemoveDeadHeartbeatsRedisScript() {
        String script = """
local deadHeartbeats = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1])
if #deadHeartbeats == 0 then
    return 0
end
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])
for i, originalId in ipairs(deadHeartbeats) do
    redis.call('ZADD', KEYS[2], ARGV[2], originalId .. ':' .. ARGV[2])
end
return #deadHeartbeats
        """;

        var redisScript = new DefaultRedisScript<Long>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    public void removeDeadHeartbeats(String roomId, long now, double maxScore) {
        String deadHeartbeatKey = redisKeyBuilder.roomHeartbeat(roomId, WaitStatus.ENTERED);
        String outflowKey = redisKeyBuilder.roomMetricOutflow(roomId);

        List<String> keys = List.of(deadHeartbeatKey, outflowKey);

        redisTemplate.execute(
                getRemoveDeadHeartbeatsRedisScript(),
                keys,
                String.valueOf(maxScore),
                String.valueOf(now)
        );
    }
}