package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.domain.room.RoomService;
import com.winten.greenlight.scheduler.scheduler.BaseScheduler;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerRegistry;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AbstractSchedulerComponent를 상속 한
 * CustomerRelocationSchedulerComponent
 * @see BaseScheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnteredCleanupScheduler extends BaseScheduler {
    private final RoomService roomService;

    @Override
    protected void registerScheduler() {
        SchedulerRegistry.register(SchedulerType.WAITING_TO_READY, this);

        scheduledTask = scheduler.scheduleWithFixedDelay(() -> {
            if (shouldStop()){
                // 현재 상태 확인 후 신규 스케줄 미동작 처리
                log.info("[ENTERED_CLEARNUP] Scheduler stopping");
                return;
            }
            try {
                roomService.removeExpiredEnteredQueue(60);
            } catch (Exception e) {
                log.error("[ENTERED_CLEARNUP] Scheduler encountered an error", e);
            }
        }, 0, 5, TimeUnit.SECONDS);

        log.info("[ENTERED_CLEARNUP] Scheduler started");
    }
}