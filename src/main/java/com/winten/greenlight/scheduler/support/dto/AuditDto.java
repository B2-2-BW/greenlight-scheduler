package com.winten.greenlight.scheduler.support.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public abstract class AuditDto {
    protected String createdBy;
    protected LocalDateTime createdAt;
    protected String updatedBy;
    protected LocalDateTime updatedAt;
}