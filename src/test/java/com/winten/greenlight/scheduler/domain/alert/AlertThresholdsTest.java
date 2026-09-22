package com.winten.greenlight.scheduler.domain.alert;

import com.winten.greenlight.scheduler.domain.room.RoomMetric;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertThresholdsTest {
    @Test
    void waitingAbsoluteCount() {
        AlertPolicy policy = AlertPolicy.builder()
                .waitingCompare(QueueAlertCompare.COUNT)
                .waitingThreshold(10d)
                .build();
        assertThat(AlertThresholds.isWaitingExceeded(policy, metric(9, 100, 100, 0))).isFalse();
        assertThat(AlertThresholds.isWaitingExceeded(policy, metric(10, 100, 100, 0))).isTrue();
    }

    @Test
    void waitingPercentOfCapacity() {
        AlertPolicy policy = AlertPolicy.builder()
                .waitingCompare(QueueAlertCompare.PERCENT)
                .waitingThreshold(50d)
                .build();
        assertThat(AlertThresholds.isWaitingExceeded(policy, metric(49, 0, 100, 0))).isFalse();
        assertThat(AlertThresholds.isWaitingExceeded(policy, metric(50, 0, 100, 0))).isTrue();
        assertThat(AlertThresholds.isWaitingExceeded(policy, metric(50, 0, 0, 0))).isFalse();
    }

    private RoomMetric metric(long waiting, long active, int capacity, long waitTime) {
        return RoomMetric.builder()
                .totalWaiting(waiting)
                .totalActive(active)
                .roomCapacity(capacity)
                .estimatedWaitTime(waitTime)
                .build();
    }
}
