package com.winten.greenlight.scheduler.domain.scheduler;

import com.winten.greenlight.scheduler.api.controller.SchedulerResponse;
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

    public List<SchedulerMeta> getSchedulersMeta() {
        var schedulerList = SchedulerRegistry.getAll();

        var metaList = new ArrayList<SchedulerMeta>();
        for (var scheduler : schedulerList) {
            var schedulerMeta = SchedulerMeta.builder()
                    .schedulerCode(scheduler.getSchedulerCode())
                    .status(scheduler.status())
                    .delaySeconds(scheduler.getDelaySeconds())
                    .name(scheduler.getName())
                    .description(scheduler.getDescription())
                    .build();
            metaList.add(schedulerMeta);
        }

        return metaList;
    }

    public void start(SchedulerCode type) {
        if (SchedulerCode.UNKNOWN == type) {
            throw new CoreException(ErrorCode.UNKNOWN_SCHEDULER_TYPE, "알 수 없는 스케쥴러 타입입니다. type: " + type);
        }
        if (SchedulerStatus.RUNNING == this.getStatus(type)) {
            throw new CoreException(ErrorCode.SCHEDULER_ALREADY_RUNNING, "스케쥴러가 이미 실행중입니다. type: " + type);
        }
        SchedulerRegistry.get(type).start();
    }

    public void stop(SchedulerCode type) {
        if (SchedulerCode.UNKNOWN == type) {
            throw new CoreException(ErrorCode.UNKNOWN_SCHEDULER_TYPE, "알 수 없는 스케쥴러 타입입니다. type: " + type);
        }
        if (SchedulerStatus.STOPPED == this.getStatus(type)) {
            throw new CoreException(ErrorCode.SCHEDULER_ALREADY_STOPPED, "스케쥴러가 이미 중단되었습니다. type: " + type);
        }
        SchedulerRegistry.get(type).stop();
    }

    public List<SchedulerResponse> getStatusList(SchedulerCode typeParam) {
        if (SchedulerCode.UNKNOWN == typeParam) {
            throw new CoreException(ErrorCode.UNKNOWN_SCHEDULER_TYPE, "알 수 없는 스케쥴러 타입입니다. type: " + typeParam);
        }
        List<SchedulerResponse> responseList = new ArrayList<>();
        for (SchedulerCode t : SchedulerCode.values()) {
            if (SchedulerCode.UNKNOWN == t) {
                continue;
            }
            if (typeParam != null && typeParam != t) { // queryParam이 전달된 경우, typeParam에 해당하는 스케쥴러의 상태만 반환
                continue;
            }
            var status = this.getStatus(t);
            var res = SchedulerResponse.builder()
                    .status(status)
                    .schedulerCode(t)
                    .build();
            responseList.add(res);
        }
        return responseList;
    }

    private SchedulerStatus getStatus(SchedulerCode type) {
        boolean isRunning = SchedulerRegistry.get(type).isRunning();
        if (isRunning) {
            return SchedulerStatus.RUNNING;
        } else {
            return SchedulerStatus.STOPPED;
        }
    }
}