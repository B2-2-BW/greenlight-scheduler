package com.winten.greenlight.scheduler.domain.scheduler;

import com.winten.greenlight.scheduler.scheduler.v2.SchedulerDelayProperties;
import com.winten.greenlight.scheduler.scheduler.v2.SchedulerRegistry;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final SchedulerDelayProperties delayProperties;

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
    }

    public void stop(SchedulerCode code) {
        var scheduler = this.getSchedulerMeta(code);
        if (SchedulerStatus.STOPPED == scheduler.getStatus()) {
            throw new CoreException(ErrorCode.SCHEDULER_ALREADY_STOPPED, "스케쥴러가 이미 중단되었습니다. type: " + code);
        }
        SchedulerRegistry.get(code).stop();
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
        scheduler.stop();
        scheduler.start();
    }
}