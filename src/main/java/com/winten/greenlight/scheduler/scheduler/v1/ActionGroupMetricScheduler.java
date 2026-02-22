package com.winten.greenlight.scheduler.scheduler.v1;

import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroupStatusService;
import com.winten.greenlight.scheduler.scheduler.AbstractScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
//@Component
@RequiredArgsConstructor
public class ActionGroupMetricScheduler extends AbstractScheduler {

    private final ActionGroupStatusService actionGroupStatusService;

    @Override
    protected void registerScheduler() {

        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            if (shouldStop()){
                // 현재 상태 확인 후 신규 스케줄 미동작 처리
                log.info("[RELOCATION] Scheduler tick: stopping");
                return;
            }

            try {
                actionGroupStatusService.recordActionGroupStatus();
            } catch (Exception e) {
                log.error("[RELOCATION] Scheduler encountered an error", e);
            }
        }, 0, 3, TimeUnit.SECONDS);

        log.info("[RELOCATION] Scheduler started");
    }
}