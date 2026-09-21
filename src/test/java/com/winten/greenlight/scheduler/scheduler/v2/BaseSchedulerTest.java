package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.domain.alert.SchedulerAlertClient;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BaseSchedulerTest {
    @Test
    void keepsRunningAndAlertsOnceAfterConsecutiveFailures() throws Exception {
        var delays = new SchedulerDelayProperties();
        delays.setDelay(SchedulerCode.METRIC, 1);
        var alertClient = mock(SchedulerAlertClient.class);
        var attempts = new AtomicInteger();
        var scheduler = new BaseScheduler(
                SchedulerCode.METRIC,
                delays,
                () -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("redis down");
                },
                SchedulePolicy.FIXED_RATE,
                alertClient
        );

        var method = BaseScheduler.class.getDeclaredMethod("safeExecute");
        method.setAccessible(true);
        method.invoke(scheduler);
        method.invoke(scheduler);
        method.invoke(scheduler);
        method.invoke(scheduler);

        assertThat(attempts.get()).isEqualTo(4);
        verify(alertClient, times(1)).send(
                eq("SCHEDULER_FAILED"),
                eq("METRIC"),
                eq(true),
                anyString(),
                anyString()
        );
    }
}
