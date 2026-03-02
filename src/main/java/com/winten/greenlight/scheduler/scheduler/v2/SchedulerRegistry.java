package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorCode;

import java.util.*;

public class SchedulerRegistry {
    private static final Map<SchedulerCode, BaseScheduler> registry = new EnumMap<>(SchedulerCode.class);

    public static void register(SchedulerCode type, BaseScheduler scheduler) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(scheduler, "scheduler must not be null");
        registry.put(type, scheduler);
    }

    public static BaseScheduler get(SchedulerCode type) {
        Objects.requireNonNull(type, "type must not be null");
        BaseScheduler scheduler = registry.get(type);
        if (scheduler == null) {
            throw CoreException.of(ErrorCode.UNKNOWN_SCHEDULER_TYPE, "No scheduler registered for type: " + type);
        }
        return scheduler;
    }

    public static List<BaseScheduler> getAll() {
        return registry.values().stream().toList();
    }
}