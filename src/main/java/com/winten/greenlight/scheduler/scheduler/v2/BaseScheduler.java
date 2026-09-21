package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.domain.alert.SchedulerAlertClient;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BaseScheduler {
    static final int FAILURE_ALERT_THRESHOLD = 3;
    static final long ERROR_LOG_INTERVAL_MS = 30_000;

    @Getter
    private final SchedulerCode schedulerCode;

    private final SchedulerDelayProperties delayProperties;

    private final Runnable task;
    private final SchedulerAlertClient alertClient;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean inFlight = new AtomicBoolean(false);

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String description;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledTask;
    private int errorCount;
    private boolean failureAlertSent;
    private boolean stoppedAlertSent;
    private long lastErrorLogAt;
    private SchedulePolicy schedulePolicy;

    /**
     * @param schedulerCode 스케줄러 타입 (Registry 등록용)
     * @param delayProperties   딜레이 설정값 (초)
     * @param task          실행할 비즈니스 로직
     */
    public BaseScheduler(SchedulerCode schedulerCode, SchedulerDelayProperties delayProperties, Runnable task) {
        this(schedulerCode, delayProperties, task, SchedulePolicy.FIXED_DELAY, null);
    }

    public BaseScheduler(SchedulerCode schedulerCode, SchedulerDelayProperties delayProperties, Runnable task, SchedulePolicy schedulePolicy) {
        this(schedulerCode, delayProperties, task, schedulePolicy, null);
    }

    public BaseScheduler(
            SchedulerCode schedulerCode,
            SchedulerDelayProperties delayProperties,
            Runnable task,
            SchedulePolicy schedulePolicy,
            SchedulerAlertClient alertClient
    ) {
        this.schedulerCode = schedulerCode;
        this.delayProperties = delayProperties;
        this.task = task;
        this.schedulePolicy = schedulePolicy;
        this.alertClient = alertClient;

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
        if (stoppedAlertSent) {
            sendAlert("SCHEDULER_STOPPED", false, "스케줄러 기동: " + schedulerCode, "스케줄러가 시작되었습니다.");
            stoppedAlertSent = false;
        }
    }

    public synchronized void stop() {
        stopInternal(true);
    }

    public synchronized void stopQuietly() {
        stopInternal(false);
    }

    private void stopInternal(boolean notify) {
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
            if (notify) {
                sendAlert("SCHEDULER_STOPPED", true, "스케줄러 중단: " + schedulerCode, "스케줄러가 중단되었습니다.");
                stoppedAlertSent = true;
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    private void safeExecute() {
        if (!inFlight.compareAndSet(false, true)) {
            log.debug("[{}] skip overlapping tick", schedulerCode);
            return;
        }
        try {
            task.run();
            errorCount = 0;
            lastErrorLogAt = 0;
            if (failureAlertSent) {
                sendAlert(
                        "SCHEDULER_FAILED",
                        false,
                        "스케줄러 복구: " + schedulerCode,
                        "연속 실패 이후 작업이 다시 성공했습니다."
                );
                failureAlertSent = false;
            }
        } catch (Exception e) {
            errorCount += 1;
            long now = System.currentTimeMillis();
            if (lastErrorLogAt == 0 || now - lastErrorLogAt >= ERROR_LOG_INTERVAL_MS) {
                log.error("[{}] 로직 실행 실패 consecutive={}", schedulerCode, errorCount, e);
                lastErrorLogAt = now;
            }
            if (errorCount >= FAILURE_ALERT_THRESHOLD && !failureAlertSent) {
                sendAlert(
                        "SCHEDULER_FAILED",
                        true,
                        "스케줄러 실패: " + schedulerCode,
                        "연속 " + errorCount + "회 실패. 스케줄러는 재시도 중. " + e.getMessage()
                );
                failureAlertSent = true;
            }
        } finally {
            inFlight.set(false);
        }
    }

    private void sendAlert(String alertname, boolean firing, String summary, String description) {
        if (alertClient == null) {
            return;
        }
        alertClient.send(alertname, schedulerCode.name(), firing, summary, description);
    }

    public SchedulerStatus status() {
        return isRunning() ? SchedulerStatus.RUNNING : SchedulerStatus.STOPPED;
    }
}