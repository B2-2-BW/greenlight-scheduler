package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.client.AdminAlertClient;
import com.winten.greenlight.scheduler.domain.alert.AlertName;
import com.winten.greenlight.scheduler.domain.alert.AlertStatus;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BaseSchedulerTest {
    @Test
    void sendsResolvedWhenFailuresRecover() throws Exception {
        var delays = new SchedulerDelayProperties();
        delays.setDelay(SchedulerCode.WAITING_TO_READY, 1);
        var alertClient = mock(AdminAlertClient.class);
        var scheduler = new BaseScheduler(
                SchedulerCode.WAITING_TO_READY,
                delays,
                () -> {},
                SchedulePolicy.FIXED_DELAY,
                alertClient,
                null
        );
        ReflectionTestUtils.setField(scheduler, "failureAlertSent", true);

        var method = BaseScheduler.class.getDeclaredMethod("safeExecute");
        method.setAccessible(true);
        method.invoke(scheduler);

        verify(alertClient).sendAlert(
                eq(AlertName.SCHEDULER_FAILED),
                eq(AlertStatus.RESOLVED),
                eq(SchedulerCode.WAITING_TO_READY),
                anyString(),
                anyString()
        );
    }

    @Test
    void resolvesOpenFailureWhenStopped() throws Exception {
        var stopped = stoppedScheduler(SchedulerCode.REMOVE_EXPIRED, true);

        stopped.scheduler().stop();

        verify(stopped.alertClient()).sendAlert(
                eq(AlertName.SCHEDULER_FAILED),
                eq(AlertStatus.RESOLVED),
                eq(SchedulerCode.REMOVE_EXPIRED),
                eq("스케쥴러 실패"),
                eq("스케쥴러: REMOVE_EXPIRED 중단")
        );
        assertThat(ReflectionTestUtils.getField(stopped.scheduler(), "failureAlertSent")).isEqualTo(false);
    }

    @Test
    void stopWithoutFailureAlertDoesNotResolve() throws Exception {
        var stopped = stoppedScheduler(SchedulerCode.CAPACITY, false);

        stopped.scheduler().stop();

        verifyNoInteractions(stopped.alertClient());
    }

    private static StoppedScheduler stoppedScheduler(SchedulerCode code, boolean failureAlertSent) throws Exception {
        var delays = new SchedulerDelayProperties();
        delays.setDelay(code, 1);
        var alertClient = mock(AdminAlertClient.class);
        var scheduler = new BaseScheduler(
                code,
                delays,
                () -> {},
                SchedulePolicy.FIXED_DELAY,
                alertClient,
                null
        );
        var executor = mock(ScheduledExecutorService.class);
        when(executor.awaitTermination(anyLong(), any())).thenReturn(true);
        var running = (AtomicBoolean) ReflectionTestUtils.getField(scheduler, "isRunning");
        running.set(true);
        ReflectionTestUtils.setField(scheduler, "executorService", executor);
        ReflectionTestUtils.setField(scheduler, "failureAlertSent", failureAlertSent);
        return new StoppedScheduler(scheduler, alertClient);
    }

    private record StoppedScheduler(BaseScheduler scheduler, AdminAlertClient alertClient) {
    }
}
