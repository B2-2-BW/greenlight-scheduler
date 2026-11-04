package com.winten.greenlight.scheduler.db.repository.redis.room;

import lombok.Getter;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class RoomRedisScript {
    private final RedisScript<Long> moveTicketRedisScript = RedisScript.of("""
local members = redis.call('ZRANGE', KEYS[1], 0, ARGV[1] - 1)
if #members == 0 then
    return 0
end

redis.call('ZREM', KEYS[1], unpack(members))
redis.call('ZREM', KEYS[4], unpack(members))

local enteredQueue = {KEYS[2]}
local enteredHeartbeat = {KEYS[3]}
for i = 1, #members do
    enteredQueue[#enteredQueue + 1] = ARGV[2]
    enteredQueue[#enteredQueue + 1] = members[i]
    enteredHeartbeat[#enteredHeartbeat + 1] = ARGV[2]
    enteredHeartbeat[#enteredHeartbeat + 1] = members[i]
end
redis.call('ZADD', unpack(enteredQueue))
redis.call('ZADD', unpack(enteredHeartbeat))
return #members
""", Long.class);

    private final RedisScript<List> roomMetricCalculationScript = RedisScript.of("""
        -- 1. 대기/활성 데이터 계산
        local totalWaiting = redis.call('ZCARD', KEYS[1])
        local totalActive = redis.call('ZCARD', KEYS[2])

        -- 2. 3초 구간 유입/입장/이탈 데이터 조회
        local counts = redis.call('MGET', KEYS[3], KEYS[4], KEYS[5], KEYS[6], KEYS[7], KEYS[8])
        local waitingIncr = tonumber(counts[1] or '0')
        local enteredIncr = tonumber(counts[2] or '0')
        local exitedIncr = tonumber(counts[3] or '0')
        local cancelledIncr = tonumber(counts[4] or '0')
        local exits = { exitedIncr, tonumber(counts[5] or '0'), tonumber(counts[6] or '0') }
        table.sort(exits)
        local medianExitRate = exits[2] / 3.0

        local state = redis.call('HMGET', KEYS[9], 'predictedExitRate', 'lastBucket')
        local predictedExitRate = tonumber(state[1])
        local lastBucket = tonumber(state[2])
        local targetBucket = tonumber(ARGV[1])
        if not lastBucket or targetBucket > lastBucket then
            if predictedExitRate then
                predictedExitRate = predictedExitRate * 0.5 + medianExitRate * 0.5
            else
                predictedExitRate = medianExitRate
            end
            redis.call('HSET', KEYS[9], 'predictedExitRate', tostring(predictedExitRate),
                'lastBucket', ARGV[1])
        end

        -- Redis는 Lua 숫자 반환값의 소수 부분을 버리므로, 예측 퇴장 인원은 문자열로 반환한다.
        return { totalWaiting, totalActive, tostring(predictedExitRate), waitingIncr, enteredIncr, exitedIncr, cancelledIncr }
    """, List.class);



    private final RedisScript<Long> increaseMetricCountByRedisScript = RedisScript.of("""
        local increment = tonumber(ARGV[1])
        local ttlSeconds = tonumber(ARGV[2])
        local count = redis.call('INCRBY', KEYS[1], increment)
        if count == increment then
            redis.call('EXPIRE', KEYS[1], ttlSeconds)
        end
        return count
    """, Long.class);

    private final RedisScript<List> getAndRemoveExpiredWaitingHeartbeatRedisScript = RedisScript.of("""
        local members = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2])
        if #members > 0 then
            redis.call('ZREM', KEYS[1], unpack(members))
        end
        return members
    """, List.class);
}
