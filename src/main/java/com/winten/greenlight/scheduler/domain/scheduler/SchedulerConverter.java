package com.winten.greenlight.scheduler.domain.scheduler;

import com.winten.greenlight.scheduler.api.controller.SchedulerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchedulerConverter {
    SchedulerResponse metaToResponse(SchedulerMeta meta);
}
