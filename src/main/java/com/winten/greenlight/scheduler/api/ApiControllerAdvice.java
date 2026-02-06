package com.winten.greenlight.scheduler.api;

import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorResponse;
import com.winten.greenlight.scheduler.support.error.ErrorCode;
import io.lettuce.core.RedisCommandTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiControllerAdvice {

    private final LettuceConnectionFactory lettuceConnectionFactory;

    @ExceptionHandler(CoreException.class)
    public ResponseEntity<ErrorResponse> handleCoreException(CoreException ex) {
        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(new ErrorResponse(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        var coreException = CoreException.of(ErrorCode.INVALID_DATA, errors);
        return handleCoreException(coreException);
    }

    @ExceptionHandler(RedisCommandTimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> redisCommandTimeoutExceptionHandler(RedisCommandTimeoutException ex) {
        lettuceConnectionFactory.resetConnection();
        throw CoreException.of(ErrorCode.REDIS_ERROR, "redis command timeout 발생. 재연결 시도");
    }
}