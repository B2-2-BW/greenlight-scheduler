package com.winten.greenlight.scheduler.scheduler.factory;

import com.winten.greenlight.scheduler.scheduler.BaseScheduler;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SchedulerRegistry {
    private static final Map<SchedulerType, BaseScheduler> registry = new HashMap<>();

    public static void register(SchedulerType type, BaseScheduler scheduler) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(scheduler, "scheduler must not be null");
        registry.put(type, scheduler);
    }

    public static BaseScheduler get(SchedulerType type) {
        Objects.requireNonNull(type, "type must not be null");
        BaseScheduler scheduler = registry.get(type);
        if (scheduler == null) {
            throw CoreException.of(ErrorCode.UNKNOWN_SCHEDULER_TYPE, "No scheduler registered for type: " + type);
        }
        return scheduler;
    }
}