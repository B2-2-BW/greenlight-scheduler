package com.winten.greenlight.scheduler.api.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SchedulerResponse {
    private SchedulerCode schedulerCode;
    private SchedulerStatus status;
    private long delaySeconds;
    private String name;
    private String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;
}