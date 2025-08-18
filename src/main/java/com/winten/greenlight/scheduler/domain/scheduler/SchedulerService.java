package com.winten.greenlight.scheduler.domain.scheduler;

import com.winten.greenlight.scheduler.api.controller.SchedulerResponse;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerFactory;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerType;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final SchedulerFactory schedulerFactory;

    public void start(SchedulerType type) {

        if (SchedulerType.UNKNOWN == type) {
            throw new CoreException(ErrorType.UNKNOWN_SCHEDULER_TYPE, "알 수 없는 스케쥴러 타입입니다. type: " + type);
        }
        if (SchedulerStatus.RUNNING == this.getStatus(type)) {
            throw new CoreException(ErrorType.SCHEDULER_ALREADY_RUNNING, "스케쥴러가 이미 실행중입니다. type: " + type);
        }
        schedulerFactory.getSchedulerComponentBy(type).start();
    }

    public void stop(SchedulerType type) {
        if (SchedulerType.UNKNOWN == type) {
            throw new CoreException(ErrorType.UNKNOWN_SCHEDULER_TYPE, "알 수 없는 스케쥴러 타입입니다. type: " + type);
        }
        if (SchedulerStatus.STOPPED == this.getStatus(type)) {
            throw new CoreException(ErrorType.SCHEDULER_ALREADY_STOPPED, "스케쥴러가 이미 중단되었습니다. type: " + type);
        }
        schedulerFactory.getSchedulerComponentBy(type).stop();
    }

    public List<SchedulerResponse> getStatusList(SchedulerType typeParam) {
        if (SchedulerType.UNKNOWN == typeParam) {
            throw new CoreException(ErrorType.UNKNOWN_SCHEDULER_TYPE, "알 수 없는 스케쥴러 타입입니다. type: " + typeParam);
        }
        List<SchedulerResponse> responseList = new ArrayList<>();
        for (SchedulerType t : SchedulerType.values()) {
            if (SchedulerType.UNKNOWN == t) {
                continue;
            }
            if (typeParam != null && typeParam != t) { // queryParam이 전달된 경우, typeParam에 해당하는 스케쥴러의 상태만 반환
                continue;
            }
            var status = this.getStatus(t);
            var res = SchedulerResponse.builder()
                    .status(status)
                    .schedulerType(t)
                    .build();
            responseList.add(res);
        }
        return responseList;
    }

    private SchedulerStatus getStatus(SchedulerType type) {
        boolean isRunning = schedulerFactory.getSchedulerComponentBy(type).isRunning();
        if (isRunning) {
            return SchedulerStatus.RUNNING;
        } else {
            return SchedulerStatus.STOPPED;
        }
    }
}