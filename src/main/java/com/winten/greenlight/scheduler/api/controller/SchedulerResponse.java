package com.winten.greenlight.scheduler.api.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import com.winten.greenlight.scheduler.scheduler.factory.SchedulerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SchedulerResponse {
    private SchedulerType schedulerType;
    private SchedulerStatus status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;
}