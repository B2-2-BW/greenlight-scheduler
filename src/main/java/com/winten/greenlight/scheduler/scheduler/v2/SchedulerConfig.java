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

    @Bean(initMethod = "start", destroyMethod = "stop")
    public BaseScheduler outflowScheduler() {
        return new BaseScheduler(
                SchedulerCode.OUTFLOW,
                5, // 5초 딜레이
                roomService::removeDeadHeartbeats
        );
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public BaseScheduler waitingToReadyScheduler() {
        return new BaseScheduler(
                SchedulerCode.WAITING_TO_READY,
                5,
                roomService::relocateCustomers
        );
    }
}
