package com.winten.greenlight.scheduler.scheduler;

import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroup;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupAccessLogService;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupService;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupStatusService;
import com.winten.greenlight.scheduler.domain.admin.service.AdminPreferenceService;
import com.winten.greenlight.scheduler.domain.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AbstractSchedulerComponent를 상속 한
 * CustomerRelocationSchedulerComponent
 * @see AbstractScheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerRelocationScheduler extends AbstractScheduler {
    private final ActionGroupService actionGroupService;
    private final CustomerService customerService;
    private final AdminPreferenceService adminPreferenceService;

    /**
     * AbstractSchedulerComponent 의 registerScheduler 상세 구현
     * CustomerRelocationScheduler 역할을 수행
     */
    @Override
    protected void registerScheduler() {
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            if (shouldStop()){
                // 현재 상태 확인 후 신규 스케줄 미동작 처리
                log.info("[RELOCATION] Scheduler tick: stopping");
                return;
            }

//            int activeCustomerDurationSeconds = adminPreferenceService.getActiveCustomerDurationSeconds();

            try {
                log.info("[RELOCATION] Scheduler tick: starting");
                List<ActionGroup> actionGroupList = actionGroupService.getAllActionGroupMeta();
                List<Integer> accessLogCountList = actionGroupService.getAllAccessLogOrdered(actionGroupList);

                for (int i = 0; i < actionGroupList.size(); i++) {
                    var actionGroup = actionGroupList.get(i);
                    var accessLogCount = accessLogCountList.get(i);

                    int availableCapacity = Math.max(actionGroup.getMaxActiveCustomers() - accessLogCount, 0);
                    // 2. 고객 재배치
                    customerService.relocateCustomerBy(actionGroup.getId(), availableCapacity);
                }
                log.info("[RELOCATION] Scheduler Relocation successful");
            } catch (Exception e) {
                log.error("[RELOCATION] Scheduler encountered an error", e);
            }
        }, 0, 1, TimeUnit.SECONDS);

        log.info("[RELOCATION] Scheduler started");
    }
}