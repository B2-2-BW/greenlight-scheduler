package com.winten.greenlight.scheduler.component.factory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SchedulerComponentType 스케줄러의 type 구분
 * @see com.winten.greenlight.scheduler.api.controller.SchedulerController
 * @see SchedulerComponentFactory
 */
@Getter
@AllArgsConstructor
public enum SchedulerComponentType {
    RELOCATION,
    CAPACITY;

    @JsonCreator
    public static SchedulerComponentType from(String value) {
        return SchedulerComponentType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }
}
