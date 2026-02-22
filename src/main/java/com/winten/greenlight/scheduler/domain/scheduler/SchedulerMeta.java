package com.winten.greenlight.scheduler.domain.scheduler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SchedulerMeta {
    private SchedulerCode schedulerCode;
    private String name;
    private String description;
    private long delaySeconds;
    private SchedulerStatus status;
}
