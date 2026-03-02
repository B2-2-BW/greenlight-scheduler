package com.winten.greenlight.scheduler.api.controller;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchedulerDelayUpdateRequest {
    @Min(value = 1, message = "스케쥴러 주기는 0보다 큰 정수여야 합니다.")
    private Integer delaySeconds;

    private boolean restart;
}