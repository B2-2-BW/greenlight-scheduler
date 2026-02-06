package com.winten.greenlight.scheduler.scheduler.factory;

import com.winten.greenlight.scheduler.scheduler.*;
import com.winten.greenlight.scheduler.scheduler.v1.ActiveUserCountScheduler;
import com.winten.greenlight.scheduler.scheduler.v1.CustomerRelocationScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SchedulerComponentType 에 따라
 * AbstractSchedulerComponent 를 상속한 적합한 SchedulerComponent type return
 * @see SchedulerType
 * @see BaseScheduler
 * @see ActiveUserCountScheduler
 * @see CustomerRelocationScheduler
 */
@Component
@RequiredArgsConstructor
public class SchedulerFactory {
    public BaseScheduler getSchedulerComponentBy(SchedulerType type) {
        return SchedulerRegistry.get(type);
    }
}