package com.winten.greenlight.scheduler.domain.scheduler;

import com.winten.greenlight.scheduler.scheduler.factory.SchedulerFactory;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final SchedulerFactory schedulerFactory;

    public void start(SchedulerType type) {
        schedulerFactory.getSchedulerComponentBy(type).start();
    }

    public void stop(SchedulerType type) {
        schedulerFactory.getSchedulerComponentBy(type).stop();
    }

    public SchedulerStatus getStatus(SchedulerType type) {
        boolean isRunning = schedulerFactory.getSchedulerComponentBy(type).isRunning();
        if (isRunning) {
            return SchedulerStatus.RUNNING;
        } else {
            return SchedulerStatus.STOPPED;
        }
    }
}