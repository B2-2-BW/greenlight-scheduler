package com.winten.greenlight.scheduler.scheduler;

import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroup;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupAccessLogService;
import com.winten.greenlight.scheduler.domain.actiongroup.service.ActionGroupService;
import com.winten.greenlight.scheduler.domain.admin.service.AdminPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 세션을 주기적으로 정리하는 스케쥴러
 * @see AbstractScheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveSessionCleanupScheduler extends AbstractScheduler {
    private final ActionGroupService actionGroupService;
    private final ActionGroupAccessLogService actionGroupAccessLogService;
    private final AdminPreferenceService adminPreferenceService;

    private final long DEFAULT_SCHEDULER_PERIOD = 5000;
    /**
     * AbstractSchedulerComponent 의 registerScheduler 상세 구현
     * ActiveSessionCleanupScheduler 역할을 수행
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

            Integer sessionDurationSeconds;
            try {
                sessionDurationSeconds = adminPreferenceService.getAdminPreference().getSessionDurationSeconds();
                if (sessionDurationSeconds == null) {
                    throw new Exception("activeDurationSeconds is null");
                }
            } catch (Exception e) {
                sessionDurationSeconds = 300;
                log.error("failed to get admin preference", e);
            }

            try {
                //1. action_group:*:meta 전체 액션 그룹 메타 데이터 호출
                List<ActionGroup> actionGroups = actionGroupService.getAllActionGroupMeta();
                //actionGroups 데이터를 통한 for 루프 시작
                for(ActionGroup actionGroup : actionGroups) {
                    //삭제: expiredMinute 지난 고객 accesslog
                    actionGroupAccessLogService.removeExpiredActionGroupSession(actionGroup.getId(), sessionDurationSeconds);
                }
                if(actionGroups.isEmpty()){
                    log.info("[CAPACITY] Scheduler NO action groups are available");
                } else {
                    log.info("[CAPACITY] Scheduler {} action groups successful",actionGroups.size());
                }
            } catch (Exception e) {
                log.error("[CAPACITY] Scheduler encountered an error", e);
            }
        }, 0, DEFAULT_SCHEDULER_PERIOD, TimeUnit.MILLISECONDS);

        log.info("[CAPACITY] Scheduler started");
    }

}