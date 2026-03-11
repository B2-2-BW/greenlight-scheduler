package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.domain.room.RoomService;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SchedulerConfig {

    private final RoomService roomService;
    private final SchedulerDelayProperties delayProperties;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public BaseScheduler metricScheduler() {
        return new BaseScheduler(
                SchedulerCode.METRIC,
                delayProperties,
                roomService::recordRoomMetric3s,
                SchedulePolicy.FIXED_RATE
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public BaseScheduler waitingToReadyScheduler() {
        return new BaseScheduler(
                SchedulerCode.WAITING_TO_READY,
                delayProperties,
                roomService::relocateCustomers,
                SchedulePolicy.FIXED_DELAY
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public BaseScheduler removeExpiredScheduler() {
        return new BaseScheduler(
                SchedulerCode.REMOVE_EXPIRED,
                delayProperties,
                roomService::removeExpired,
                SchedulePolicy.FIXED_RATE
        );
    }
}