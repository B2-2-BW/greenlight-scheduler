package com.winten.greenlight.scheduler.scheduler.v1;

import com.winten.greenlight.scheduler.domain.actionevent.ActionEventService;
import com.winten.greenlight.scheduler.scheduler.BaseScheduler;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerRegistry;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * AbstractSchedulerComponent를 상속 한
 * CustomerRelocationSchedulerComponent
 * @see BaseScheduler
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class RedisCleanupScheduler extends BaseScheduler {
    private final ActionEventService actionEventService;

    @Override
    protected void registerScheduler() {
        SchedulerRegistry.register(SchedulerType.REDIS_CLEANUP, this);

        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            if (shouldStop()){
                // 현재 상태 확인 후 신규 스케줄 미동작 처리
                log.info("[RELOCATION] Scheduler tick: stopping");
                return;
            }

            try {
                actionEventService.trimActionEventStream();
            } catch (Exception e) {
                log.error("[RELOCATION] Scheduler encountered an error", e);
            }
        }, 0, 600, TimeUnit.SECONDS);

        log.info("[RELOCATION] Scheduler started");
    }
}