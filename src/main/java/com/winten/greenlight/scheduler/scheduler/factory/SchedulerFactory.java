package com.winten.greenlight.scheduler.scheduler.factory;

import com.winten.greenlight.scheduler.scheduler.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SchedulerComponentType 에 따라
 * AbstractSchedulerComponent 를 상속한 적합한 SchedulerComponent type return
 * @see SchedulerType
 * @see AbstractScheduler
 * @see ActiveUserCountScheduler
 * @see CustomerRelocationScheduler
 */
@Component
@RequiredArgsConstructor
public class SchedulerFactory {
    private final CustomerRelocationScheduler customerRelocationScheduler;
    private final ActiveUserCountScheduler activeUserCountScheduler;
    private final ActiveSessionCleanupScheduler activeSessionCleanupScheduler;
    private final RedisCleanupScheduler redisCleanupScheduler;
    private final ActionGroupMetricScheduler actionGroupMetricScheduler;

    public AbstractScheduler getSchedulerComponentBy(SchedulerType type) {
        return switch (type) {
            case RELOCATION -> customerRelocationScheduler;
            case CAPACITY -> activeUserCountScheduler;
            case CLEANUP_SESSION -> activeSessionCleanupScheduler;
            case REDIS_CLEANUP -> redisCleanupScheduler;
            case METRIC -> actionGroupMetricScheduler;
            default -> throw new IllegalArgumentException("Unknown scheduler type: [" + type + "]");
        };
    }
}