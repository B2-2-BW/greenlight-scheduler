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
    RELOCATION, /* 고객 이동 스케쥴러 */
    CAPACITY, /* 대기열 활성사용자수 계산 스케쥴러 */
    CLEANUP_SESSION, /* session 정리 컨트롤러 */
    UNKNOWN /* 알 수 없음 */
    ;

    @JsonCreator
    public static SchedulerType from(String value) {
        return SchedulerType.valueOf(value.toUpperCase());
    }

    public static SchedulerType of(String source) {
        try {
            return SchedulerType.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }
}