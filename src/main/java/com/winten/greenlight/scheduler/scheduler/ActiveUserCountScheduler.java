package com.winten.greenlight.scheduler.scheduler;

import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroup;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupAccessLogService;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupService;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupStatusService;
import com.winten.greenlight.scheduler.domain.admin.service.AdminPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AbstractSchedulerComponent를 상속 한
 * ActionGroupActiveUserSchedulerComponent
 * @see AbstractScheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveUserCountScheduler extends AbstractScheduler {
    private final ActionGroupService actionGroupService;
    private final ActionGroupStatusService actionGroupStatusService;
    private final ActionGroupAccessLogService actionGroupAccessLogService;
    private final AdminPreferenceService adminPreferenceService;

    /**
     * AbstractSchedulerComponent 의 registerScheduler 상세 구현
     * ActionGroupActiveUserScheduler 역할을 수행
     */
    @Override
    protected void registerScheduler() {
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            if (shouldStop()){
                // 현재 상태 확인 후 신규 스케줄 미동작 처리
                log.info("[CAPACITY] Scheduler tick: stopping");
                //log.info("[CAPACITY] Scheduler tick: stopped",Sch);
                return;
            }

            log.info("[CAPACITY] Scheduler tick: starting");

            //0.어드민 화면에 구현 된 사용자 경험 우선(3초)/서버 안정성 우선(5초) 별도 redis 키를 통해 가져옴
            Integer activeDurationSeconds;
            try {
                activeDurationSeconds = adminPreferenceService.getAdminPreference().getActiveCustomerDurationSeconds();
                if (activeDurationSeconds == null) {
                    throw new Exception("activeDurationSeconds is null");
                }
            } catch (Exception e) {
                activeDurationSeconds = 5;
                log.error("failed to get admin preference", e);
            }
            log.info("[CAPACITY] Scheduler: Current active customers expired seconds: {}", activeDurationSeconds);

            try {
                //1. action_group:*:meta 전체 액션 그룹 메타 데이터 호출
                List<ActionGroup> actionGroups = actionGroupService.getAllActionGroupMeta();
                //actionGroups 데이터를 통한 for 루프 시작
                for(ActionGroup actionGroup : actionGroups) {
                    //삭제: expiredMinute 지난 고객 accesslog
                    actionGroupAccessLogService.removeOverExpireMinuteCustomersFromActionGroupAccessLogBy(actionGroup.getId(), activeDurationSeconds);

                    //집계/계산: {actionGroupId} 별 action_group:{actionGroupId}:accesslog 키의 활성 사용자 수(currentActiveCustomers)
                    int maxActiveCustomers = actionGroup.getMaxActiveCustomers();
                    int currentActiveCustomers = actionGroupAccessLogService.getCurrentActiveCustomersCountFromActionGroupAccessLogBy(actionGroup.getId());
                    Long waitingQueueCount = actionGroupService.getWaitingQueueCountByActionGroupId(actionGroup.getId());
                    int availableCapacity = Math.max(maxActiveCustomers - currentActiveCustomers - waitingQueueCount.intValue(), 0);

                    //저장: key= action_group:{actionGroup.getId()}:status 의 value=availableCapacity, currentActiveCustomers의 json 양식
                    actionGroupStatusService.saveActionGroupStatusBy(actionGroup.getId(), currentActiveCustomers, availableCapacity);

                    log.info("[CAPACITY] Scheduler SAVED action_group:{}:status successful",actionGroup.getId());
                }
                if(actionGroups.isEmpty()){
                    log.info("[CAPACITY] Scheduler NO action groups are available");
                } else {
                    log.info("[CAPACITY] Scheduler {} action groups successful",actionGroups.size());
                }
            } catch (Exception e) {
                log.error("[CAPACITY] Scheduler encountered an error", e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        log.info("[CAPACITY] Scheduler started");
    }

}