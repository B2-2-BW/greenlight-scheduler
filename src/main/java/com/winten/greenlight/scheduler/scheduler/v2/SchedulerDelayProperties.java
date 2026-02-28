package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scheduler")
public class SchedulerDelayProperties {
    private final Map<SchedulerCode, Integer> delays = new EnumMap<>(SchedulerCode.class);

    public int getDelay(SchedulerCode code) {
         return delays.get(code);
    }

    public void setDelay(SchedulerCode code, Integer delaySeconds) {
        if (delaySeconds == null || delaySeconds < 1) {
            throw CoreException.of(ErrorCode.SCHEDULER_ALREADY_RUNNING, "스케쥴러 주기는 0보다 큰 정수여야 합니다.");
        }
        delays.put(code, delaySeconds);
    }
}