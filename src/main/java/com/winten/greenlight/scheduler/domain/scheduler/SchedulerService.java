package com.winten.greenlight.scheduler.domain.scheduler;

import com.winten.greenlight.scheduler.client.AdminAlertClient;
import com.winten.greenlight.scheduler.domain.alert.AlertName;
import com.winten.greenlight.scheduler.domain.alert.AlertStatus;
import com.winten.greenlight.scheduler.scheduler.v2.SchedulerDelayProperties;
import com.winten.greenlight.scheduler.scheduler.v2.SchedulerRegistry;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final SchedulerDelayProperties delayProperties;
    private final AdminAlertClient adminAlertClient;

    public List<SchedulerMeta> getSchedulerMetaList() {
        var schedulerList = SchedulerRegistry.getAll();

        var metaList = new ArrayList<SchedulerMeta>();
        for (var scheduler : schedulerList) {
            var schedulerMeta = SchedulerMeta.builder()
                    .schedulerCode(scheduler.getSchedulerCode())
                    .status(scheduler.status())
                    .delaySeconds(delayProperties.getDelay(scheduler.getSchedulerCode()))
                    .name(scheduler.getName())
                    .description(scheduler.getDescription())
                    .build();
            metaList.add(schedulerMeta);
        }

        return metaList;
    }

    public SchedulerMeta getSchedulerMeta(SchedulerCode code) {
        if (SchedulerCode.UNKNOWN == code) {
            throw new CoreException(ErrorCode.UNKNOWN_SCHEDULER_TYPE, "알 수 없는 스케쥴러 타입입니다.");
        }
        var scheduler = SchedulerRegistry.get(code);
        return SchedulerMeta.builder()
                .schedulerCode(scheduler.getSchedulerCode())
                .status(scheduler.status())
                .delaySeconds(delayProperties.getDelay(scheduler.getSchedulerCode()))
                .name(scheduler.getName())
                .description(scheduler.getDescription())
                .build();
    }

    public void start(SchedulerCode code) {
        var scheduler = this.getSchedulerMeta(code);
        if (SchedulerStatus.RUNNING == scheduler.getStatus()) {
            throw new CoreException(ErrorCode.SCHEDULER_ALREADY_RUNNING, "스케쥴러가 이미 실행중입니다. type: " + code);
        }
        SchedulerRegistry.get(code).start();
        notifySchedulerLifecycle(code, false);
    }

    public void stop(SchedulerCode code) {
        var scheduler = this.getSchedulerMeta(code);
        if (SchedulerStatus.STOPPED == scheduler.getStatus()) {
            throw new CoreException(ErrorCode.SCHEDULER_ALREADY_STOPPED, "스케쥴러가 이미 중단되었습니다. type: " + code);
        }
        SchedulerRegistry.get(code).stop();
        notifySchedulerLifecycle(code, true);
    }

    public SchedulerMeta updateDelay(final SchedulerCode schedulerCode, final Integer delaySeconds, boolean restart) {
        delayProperties.setDelay(schedulerCode, delaySeconds);

        if (restart) {
            this.restart(schedulerCode);
        }

        return this.getSchedulerMeta(schedulerCode);
    }

    public void restart(final SchedulerCode schedulerCode) {
        var scheduler = SchedulerRegistry.get(schedulerCode);
        boolean wasRunning = scheduler.isRunning();
        scheduler.stop();
        if (wasRunning) {
            notifySchedulerLifecycle(schedulerCode, true);
        }
        scheduler.start();
        notifySchedulerLifecycle(schedulerCode, false);
    }

    private void notifySchedulerLifecycle(SchedulerCode code, boolean stopped) {
        try {
            if (stopped) {
                adminAlertClient.sendAlert(
                        AlertName.SCHEDULER_STOPPED,
                        AlertStatus.FIRING,
                        code,
                        "[" + code + "] 스케쥴러 중단",
                        "스케쥴러가 비활성화되었습니다."
                );
            } else {
                adminAlertClient.sendAlert(
                        AlertName.SCHEDULER_STOPPED,
                        AlertStatus.RESOLVED,
                        code,
                        "[" + code + "] 스케쥴러 활성화",
                        "스케쥴러가 활성화되었습니다."
                );
            }
        } catch (Exception exception) {
            log.error("[{}] 스케쥴러 {} 알람 발송 실패", code, stopped ? "중단" : "활성화", exception);
        }
    }
}