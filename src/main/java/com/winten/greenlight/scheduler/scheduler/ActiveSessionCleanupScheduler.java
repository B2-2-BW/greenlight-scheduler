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
    /**
     * AbstractSchedulerComponent 의 registerScheduler 상세 구현
     * ActiveSessionCleanupScheduler 역할을 수행
     */
    @Override
    protected void registerScheduler() {
        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            if (shouldStop()){
                // 현재 상태 확인 후 신규 스케줄 미동작 처리
                log.info("[SESSION_CLEANUP] Scheduler tick: stopping");
                //log.info("[SESSION_CLEANUP] Scheduler tick: stopped",Sch);
                return;
            }

            log.info("[SESSION_CLEANUP] Scheduler tick: starting");
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
                actionGroupAccessLogService.removeExpiredActionGroupSession(sessionDurationSeconds);
            } catch (Exception e) {
                log.error("[SESSION_CLEANUP] Scheduler encountered an error", e);
            }
        }, 0, 5000, TimeUnit.MILLISECONDS);

        log.info("[SESSION_CLEANUP] Scheduler started");
    }

}