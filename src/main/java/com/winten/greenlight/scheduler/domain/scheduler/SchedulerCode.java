package com.winten.greenlight.scheduler.domain.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SchedulerComponentType 스케줄러의 type 구분
 * @see com.winten.greenlight.scheduler.api.controller.SchedulerController
 */
@Getter
@AllArgsConstructor
public enum SchedulerCode {
    WAITING_TO_READY, /* 고객 입장처리 스케쥴러 (v2) */
    METRIC, /* 화면 이탈량 계산 스케쥴러 (v2) */
    REMOVE_EXPIRED, /* 만료된 데이터 삭제 스케쥴러 (v2) */

    RELOCATION, /* 고객 이동 스케쥴러 */
    CAPACITY, /* 대기열 활성사용자수 계산 스케쥴러 */
    CLEANUP_SESSION, /* session 정리 스케쥴러 */
    REDIS_CLEANUP, /* Redis stream 정리 스케쥴러 */
    @JsonIgnore
    UNKNOWN /* 알 수 없음 */
    ;

    @JsonCreator
    public static SchedulerCode from(String value) {
        return SchedulerCode.valueOf(value.toUpperCase());
    }

    public static SchedulerCode of(String source) {
        try {
            return SchedulerCode.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    @JsonValue
    public String toValue() {
        return this.name().toUpperCase();
    }
}