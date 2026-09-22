package com.winten.greenlight.scheduler.scheduler.v2;

import com.winten.greenlight.scheduler.client.AdminAlertClient;
import com.winten.greenlight.scheduler.domain.alert.AlertName;
import com.winten.greenlight.scheduler.domain.alert.AlertStatus;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
