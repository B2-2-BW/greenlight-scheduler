package com.winten.greenlight.scheduler.domain.scheduler;

import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerRunningStatus {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    public void save(SchedulerCode schedulerCode, boolean enabled) {
        try {
            redisTemplate.opsForValue().set(
                    redisKeyBuilder.schedulerEnabled(schedulerCode.name()),
                    Boolean.toString(enabled)
            );
        } catch (Exception exception) {
            log.error("Failed to store scheduler running status. code={} enabled={}", schedulerCode, enabled, exception);
        }
    }
}
