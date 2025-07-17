package com.winten.greenlight.scheduler.domain.actiongroup.service;

import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroup;
import com.winten.greenlight.scheduler.domain.customer.CustomerService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionGroupActiveUserSchedulerService {
    private final ActionGroupService actionGroupService;
    private final ActionGroupStatusService actionGroupStatusService;
    private final ActionGroupAccessLogService actionGroupAccessLogService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;

    @PostConstruct
    public void scheduleActiveCustomersCalculate() {
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("Scheduler tick: starting [scheduleActiveCustomersCalculate]");

                //FIXME 0.어드민 화면에 구현 된 사용자 경험 우선(3초)/서버 안정성 우선(5초) 별도 redis 키를 통해 가져올 예정, 현재는 5초 하드코딩
                int expiredSeconds = 5;
                //1. action_group:*:meta 전체 액션 그룹 메타 데이터 호출
                List<ActionGroup> actionGroups = actionGroupService.getAllActionGroupMeta();
                //actionGroups 데이터를 통한 for 루프 시작
                for(ActionGroup actionGroup : actionGroups) {
                    //삭제: expiredMinute 지난 고객 accesslog
                    actionGroupAccessLogService.removeOverExpireMinuteCustomersFromActionGroupAccessLogBy(actionGroup.getId(), expiredSeconds);

                    //집계/계산: {actionGroupId} 별 action_group:{actionGroupId}:accesslog 키의 활성 사용자 수(currentActiveCustomers)
                    int maxActiveCustomers = actionGroup.getMaxActiveCustomers();
                    int currentActiveCustomers = actionGroupAccessLogService.getCurrentActiveCustomersCountFromActionGroupAccessLogBy(actionGroup.getId());
                    int availableCapacity = maxActiveCustomers - currentActiveCustomers;

                    //저장: key= action_group:{actionGroup.getId()}:status 의 value=availableCapacity, currentActiveCustomers의 json 양식
                    actionGroupStatusService.saveActionGroupStatusBy(actionGroup.getId(), currentActiveCustomers, availableCapacity);

                    log.info("[scheduleActiveCustomersCalculate] SAVED action_group:{}:status successful",actionGroup.getId());
                }
                if(actionGroups.isEmpty()){
                    log.info("[scheduleActiveCustomersCalculate] NO action groups are available");
                } else {
                    log.info("[scheduleActiveCustomersCalculate] {} action groups successful",actionGroups.size());
                }

            } catch (Exception e) {
                log.error("[scheduleActiveCustomersCalculate] encountered an error", e);
            }
        }, 0, 1, TimeUnit.SECONDS);

        log.info("[scheduleActiveCustomersCalculate] started");
    }

    @PreDestroy
    public void shutdownScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }
        scheduler.shutdown();
        log.info("[scheduleActiveCustomersCalculate] stopped");
    }

    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }

    public void startScheduler() {
        if (scheduledTask == null || scheduledTask.isCancelled()) {
            scheduleActiveCustomersCalculate();
        }
    }

    public void stopScheduler() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }
    }
}
