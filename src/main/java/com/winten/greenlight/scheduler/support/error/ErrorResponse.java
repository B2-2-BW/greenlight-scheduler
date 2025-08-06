package com.winten.greenlight.scheduler.support.error;

import org.springframework.http.HttpStatus;

public record ErrorResponse(HttpStatus status, String message, Object detail) {
    public ErrorResponse(CoreException exception) {
        this(exception.getErrorType().getStatus(),
                exception.getErrorType().getMessage(),
                exception.getDetail()
        );
    }
}