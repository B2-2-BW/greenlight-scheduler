package com.winten.greenlight.scheduler.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * AbstractSchedulerComponent
 * Base Scheduler Component
 */
@Slf4j
public abstract class AbstractScheduler {
    protected ScheduledFuture<?> scheduledTask;
    protected final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    /**
     * 추가된 안전 중단용 플래그
     * volatile: 변수 값을 모든 스레드가 항상 '최신 상태'로 보도록 보장해주는 키워드
     */
    protected volatile boolean isStopped = false;

    @PostConstruct
    protected abstract void registerScheduler();

    @PreDestroy
    public void shutdownScheduler() {
        stop();
        scheduler.shutdown();
    }

    /**
     * 스케줄러 동작 상태 체크
     * @return true/false
     */
    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isDone();
    }

    /**
     * 스케줄러 start
     * isStopped = false;
     * 스케줄러 실행
     */
    public void start() {
        if (scheduledTask == null || scheduledTask.isDone()) {
            isStopped = false;
            registerScheduler();
        }
    }

    /**
     * 스케줄러 stop
     * 최종 인터럽트까지는 실행 되도록 cancel true 가 아닌 false로 변경
     */
    public void stop() {
        isStopped = true;
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }
    }

    /**
     * 스케줄러 내 반복 작업 코드에서 아래 메서드로 상태 확인 후 안전 종료 처리
     * if (shouldStop()) return;
     */
    protected boolean shouldStop() {
        return isStopped || Thread.currentThread().isInterrupted();
    }
}