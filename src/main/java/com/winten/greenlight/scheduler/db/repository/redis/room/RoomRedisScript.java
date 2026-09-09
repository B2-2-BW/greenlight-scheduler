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
if #members > 0 then
    redis.call('ZREM', KEYS[1], unpack(members))
    redis.call('ZREM', KEYS[4], unpack(members))
    for _, member in ipairs(members) do
        redis.call('ZADD', KEYS[2], ARGV[2], member)
        redis.call('ZADD', KEYS[3], ARGV[2], member)
    end
    return #members
else
    return 0
end
""", Long.class);

    private final RedisScript<List> roomMetricCalculationScript = RedisScript.of("""
        -- 1. 대기/활성 데이터 계산 (현재 ZSET 크기. 3초 버킷 스냅샷이 아님)
        local totalWaiting = redis.call('ZCARD', KEYS[1])
        local totalActive = redis.call('ZCARD', KEYS[2])

        -- 2. 3초 구간 유입/입장/이탈 데이터 조회
        local waitingIncr = tonumber(redis.call('GET', KEYS[3]) or '0')
        local enteredIncr = tonumber(redis.call('GET', KEYS[4]) or '0')
        local exitedIncr = tonumber(redis.call('GET', KEYS[5]) or '0')
        local cancelledIncr = tonumber(redis.call('GET', KEYS[6]) or '0')

        -- 3. EXITED 60개 버킷 리스트 추출 (KEYS[6] 부터 끝까지)
        local exitedKeys = {}
        for i = 7, #KEYS do
            exitedKeys[#exitedKeys + 1] = KEYS[i]
        end

        -- 4. MGET으로 60개 키를 한 번에 조회 후 합산
        local recentlyExited = 0
        if #exitedKeys > 0 then
            -- unpack은 배열을 인자 목록으로 전개합니다
            local exitedVals = redis.call('MGET', unpack(exitedKeys))
            for _, val in ipairs(exitedVals) do
                if val then
                    recentlyExited = recentlyExited + tonumber(val)
                end
            end
        end

        return { totalWaiting, totalActive, recentlyExited, waitingIncr, enteredIncr, exitedIncr, cancelledIncr }
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
