package com.winten.greenlight.scheduler.scheduler.factory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SchedulerComponentType 스케줄러의 type 구분
 * @see com.winten.greenlight.scheduler.api.controller.SchedulerController
 * @see SchedulerFactory
 */
@Getter
@AllArgsConstructor
public enum SchedulerType {
    RELOCATION,
    CAPACITY;

    @JsonCreator
    public static SchedulerType from(String value) {
        return SchedulerType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }
}
