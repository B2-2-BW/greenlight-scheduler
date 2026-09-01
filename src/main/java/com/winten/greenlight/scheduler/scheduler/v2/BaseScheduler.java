package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.client.AdminAlertClient;
import com.winten.greenlight.scheduler.domain.alert.AlertName;
import com.winten.greenlight.scheduler.domain.alert.AlertStatus;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import io.lettuce.core.RedisException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BaseScheduler {

    @Getter
    private final SchedulerCode schedulerCode;

    private final SchedulerDelayProperties delayProperties;

    private final Runnable task;

    private final AdminAlertClient adminAlertClient;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String description;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledTask;
    private int errorCount = 0;
    private SchedulePolicy schedulePolicy;
    private long alertLastSentAt = 0;

    public BaseScheduler(SchedulerCode schedulerCode,
                         SchedulerDelayProperties delayProperties,
                         Runnable task,
                         SchedulePolicy schedulePolicy,
                         AdminAlertClient adminAlertClient
    ) {
        if (schedulePolicy == SchedulePolicy.FIXED_RATE) {
            // REDIS 오류 발생 시 thread sleep을 실행하게 되는데, 이 때 FIXED RATE로 실행할 경우 누적 실패가 발생하므로 일단 사용되지 않도록 조치
            throw new IllegalArgumentException("SchedulePolicy.FIXED_RATE is not supported.");
        }
        this.schedulerCode = schedulerCode;
        this.delayProperties = delayProperties;
        this.task = task;
        this.schedulePolicy = schedulePolicy;
        this.adminAlertClient = adminAlertClient;

        // 초기화 시 Registry에 자동 등록
        SchedulerRegistry.register(this.schedulerCode, this);
    }


    public synchronized void start() {
        if (isRunning.get()) {
            return; // 이미 실행 중이면 무시
        }

        // Java 21+ 가상 스레드 활용 (선택사항: CPU 바운드라면 일반 스레드 권장)
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name(schedulerCode.name() + "-thread-", 0)
                .factory();

        this.executorService = Executors.newSingleThreadScheduledExecutor(virtualThreadFactory);

        // initialDelay 5초
        if (schedulePolicy == SchedulePolicy.FIXED_DELAY) {
            this.scheduledTask = this.executorService.scheduleWithFixedDelay(
                    this::safeExecute,
                    5,
                    delayProperties.getDelay(this.schedulerCode),
                    TimeUnit.SECONDS
            );
        } else {
            this.scheduledTask = this.executorService.scheduleAtFixedRate(
                    this::safeExecute,
                    5,
                    delayProperties.getDelay(this.schedulerCode),
                    TimeUnit.SECONDS
            );
        }

        errorCount = 0;
        isRunning.set(true);
        log.info("[{}] 스케쥴러 시작 완료", schedulerCode);
    }

    public synchronized void stop() {
        if (!isRunning.get() || executorService == null) {
            return;
        }

        // 1. 새로운 작업 예약 중단
        executorService.shutdown();

        try {
            // 2. 현재 실행 중인 작업이 끝날 때까지 대기 (Graceful Stop)
            // 비즈니스 로직의 최대 실행 시간을 고려하여 타임아웃 설정 (예: 30초)
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                // 타임아웃 초과 시 강제 종료 (InterruptedException 발생시킴)
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            log.info("[{}] 스케쥴러 중단 완료", schedulerCode);
            isRunning.set(false);
            scheduledTask = null;
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    // 예외가 발생하더라도 스케줄러가 죽지 않도록 방어 로직 추가
    private void safeExecute() {
        try {
            task.run();
            errorCount = 0;
        } catch (Exception e) {
            errorCount += 1;
            long backoff = Math.min(errorCount * 3, 30);
            if (e instanceof RedisException) {
                log.warn("[{}] Redis 오류 연속 {}회 발생. {}초간 일시중단합니다. {}", schedulerCode, errorCount, backoff, e.toString());
            } else {
                log.error("[{}] 스케쥴러 연속 {}회 실패. {}초간 일시중단합니다.", schedulerCode, errorCount, backoff, e);
            }
            long now = System.currentTimeMillis();
            // 알람은 5분에 한번만 발송
            if (errorCount > 3 && alertLastSentAt < now - 300_000) {
                log.error("[{}] 스케쥴러 실패 알람 발송 {}.", schedulerCode, LocalDateTime.now());
                String message = "스케쥴러 실행 연속 " + errorCount + "회 실패. lastError: " + e;
                adminAlertClient.sendAlert(
                        AlertName.SCHEDULER_FAILED,
                        AlertStatus.FIRING,
                        schedulerCode,
                        "[" + schedulerCode + "] 스케쥴러 실행 실패",
                        message
                );
                alertLastSentAt = now;
            }
            sleep(backoff);
        }
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public SchedulerStatus status() {
        return isRunning() ? SchedulerStatus.RUNNING : SchedulerStatus.STOPPED;
    }
}