package com.winten.greenlight.scheduler.component;

import com.winten.greenlight.scheduler.component.base.AbstractSchedulerComponent;
import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroupStatus;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupStatusService;
import com.winten.greenlight.scheduler.domain.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

/**
 * AbstractSchedulerComponent를 상속 한
 * CustomerRelocationSchedulerComponent
 * @see AbstractSchedulerComponent
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRelocationSchedulerComponent extends AbstractSchedulerComponent {
    private final ActionGroupStatusService actionGroupStatusService;
    private final CustomerService customerService;

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

            try {
                log.info("[RELOCATION] Scheduler tick: starting");

                //1. action_group:*:status 전체 액션 그룹 status 데이터 호출
                List<ActionGroupStatus> arrAllActionGroupStatus = actionGroupStatusService.getAllActionGroupStatus();
                for(ActionGroupStatus actionGroupStatus : arrAllActionGroupStatus) {
                    Long actionGroupId = actionGroupStatus.getId();
                    Long availableCapacity = actionGroupStatus.getAvailableCapacity();

                    // 2. 고객 재배치
                    customerService.relocateCustomerBy(actionGroupId, availableCapacity);
                }

                log.info("[RELOCATION] Scheduler Relocation successful");

            } catch (Exception e) {
                log.error("[RELOCATION] Scheduler encountered an error", e);
            }
        }, 0, 5, TimeUnit.SECONDS);

        log.info("[RELOCATION] Scheduler started");
    }
}
