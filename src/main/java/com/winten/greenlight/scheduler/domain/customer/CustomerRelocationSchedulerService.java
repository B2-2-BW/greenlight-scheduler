package com.winten.greenlight.scheduler.domain.customer;

import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroupStatus;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupStatusService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRelocationSchedulerService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;
    private final ActionGroupStatusService actionGroupStatusService;
    private final CustomerService customerService;


    @PostConstruct
    public void scheduleCustomerRelocation() {
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("Scheduler tick: starting relocation");

                //1. action_group:*:status 전체 액션 그룹 status 데이터 호출
                List<ActionGroupStatus> arrAllActionGroupStatus = actionGroupStatusService.getAllActionGroupStatus();
                for(ActionGroupStatus actionGroupStatus : arrAllActionGroupStatus) {
                    Long actionGroupId = actionGroupStatus.getId();
                    Long availableCapacity = actionGroupStatus.getAvailableCapacity();

                    // 2. 고객 재배치
                    customerService.relocateCustomerBy(actionGroupId, availableCapacity);
                }

                log.info("Relocation successful");

            } catch (Exception e) {
                log.error("Scheduler encountered an error", e);
            }
        }, 0, 5, TimeUnit.SECONDS);

        log.info("CustomerRelocationScheduler started");
    }

    @PreDestroy
    public void shutdownScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }
        scheduler.shutdown();
        log.info("CustomerRelocationScheduler stopped");
    }

    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }

    public void startScheduler() {
        if (scheduledTask == null || scheduledTask.isCancelled()) {
            scheduleCustomerRelocation();
        }
    }

    public void stopScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }
    }
}
