package com.winten.greenlight.scheduler.domain.scheduler;

import com.winten.greenlight.scheduler.client.AdminAlertClient;
import com.winten.greenlight.scheduler.domain.alert.AlertName;
import com.winten.greenlight.scheduler.domain.alert.AlertSeverity;
import com.winten.greenlight.scheduler.domain.alert.AlertStatus;
import com.winten.greenlight.scheduler.scheduler.v2.SchedulerDelayProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchedulerServiceTest {
    @Test
    void startResolvesStopWithTheSameWarningSeverity() throws Exception {
        var alertClient = mock(AdminAlertClient.class);
        var service = new SchedulerService(new SchedulerDelayProperties(), alertClient);
        var notify = SchedulerService.class.getDeclaredMethod(
                "notifySchedulerLifecycle", SchedulerCode.class, boolean.class);
        notify.setAccessible(true);

        notify.invoke(service, SchedulerCode.METRIC, true);
        notify.invoke(service, SchedulerCode.METRIC, false);

        verify(alertClient).sendAlert(
                eq(AlertName.SCHEDULER_STOPPED),
                eq(AlertStatus.FIRING),
                eq(SchedulerCode.METRIC),
                eq("스케쥴러 중단"),
                eq("스케쥴러: METRIC 중단"),
                eq(AlertSeverity.WARNING)
        );
        verify(alertClient).sendAlert(
                eq(AlertName.SCHEDULER_STOPPED),
                eq(AlertStatus.RESOLVED),
                eq(SchedulerCode.METRIC),
                eq("스케쥴러 실행"),
                eq("스케쥴러: METRIC 실행"),
                eq(AlertSeverity.WARNING)
        );
    }
}
