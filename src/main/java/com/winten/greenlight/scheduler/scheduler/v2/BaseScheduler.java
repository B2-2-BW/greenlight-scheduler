package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.domain.scheduler.SchedulerStatus;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BaseScheduler {

    @Getter
    private final SchedulerCode schedulerCode;

    private final SchedulerDelayProperties delayProperties;

    private final Runnable task;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String description;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledTask;
    private int errorCount;
    private SchedulePolicy schedulePolicy;

    /**
     * @param schedulerCode 스케줄러 타입 (Registry 등록용)
     * @param delayProperties   딜레이 설정값 (초)
     * @param task          실행할 비즈니스 로직
     */
    public BaseScheduler(SchedulerCode schedulerCode, SchedulerDelayProperties delayProperties, Runnable task) {
        this(schedulerCode, delayProperties, task, SchedulePolicy.FIXED_DELAY);
    }

    public BaseScheduler(SchedulerCode schedulerCode, SchedulerDelayProperties delayProperties, Runnable task, SchedulePolicy schedulePolicy) {
        this.schedulerCode = schedulerCode;
        this.delayProperties = delayProperties;
        this.task = task;
        this.schedulePolicy = schedulePolicy;

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
            log.error("[{}] 로직 실행 실패: {}", schedulerCode, e.getMessage());
            errorCount += 1;
            if (errorCount == 3) {
                this.stop();
                log.error("[{}] 작업 실패가 지속되어 스케줄러를 종료합니다.", schedulerCode);
            }
        }
    }

    public SchedulerStatus status() {
        return isRunning() ? SchedulerStatus.RUNNING : SchedulerStatus.STOPPED;
    }
}