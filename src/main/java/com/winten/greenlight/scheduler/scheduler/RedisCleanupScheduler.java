package com.winten.greenlight.scheduler.scheduler;

import com.winten.greenlight.scheduler.domain.actionevent.ActionEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AbstractSchedulerComponent를 상속 한
 * CustomerRelocationSchedulerComponent
 * @see AbstractScheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCleanupScheduler extends AbstractScheduler {
    private final ActionEventService actionEventService;

    @Override
    protected void registerScheduler() {
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