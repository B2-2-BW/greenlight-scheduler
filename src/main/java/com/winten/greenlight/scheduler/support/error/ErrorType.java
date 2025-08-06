package com.winten.greenlight.scheduler.support.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorType {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred.", LogLevel.ERROR),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Event not found.", LogLevel.INFO),
    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error has occurred while accessing data." , LogLevel.ERROR ),
    INVALID_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "Data is not valid." , LogLevel.WARN ),
    JSON_CONVERT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Json conversion error", LogLevel.WARN ),
    SCHEDULER_ALREADY_RUNNING(HttpStatus.CONFLICT, "Scheduler is already running." , LogLevel.INFO),
    SCHEDULER_ALREADY_STOPPED(HttpStatus.CONFLICT, "Scheduler is already stopped." , LogLevel.INFO),
    UNKNOWN_SCHEDULER_TYPE(HttpStatus.BAD_REQUEST, "Invalid scheduler type" , LogLevel.INFO),
    ;

    private final HttpStatus status; //HTTP 응답 코드
    private final String message; // 노출 메시지
    private final LogLevel logLevel;

}