package com.winten.greenlight.scheduler.domain.alert;

import com.winten.greenlight.scheduler.domain.room.RoomMetric;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertThresholdsTest {
    @Test
    void queueWaitAbsoluteWaiting() {
        AlertPolicy policy = AlertPolicy.builder()
                .queueMetric(QueueAlertMetric.WAITING)
                .queueCompare(QueueAlertCompare.COUNT)
                .queueThreshold(10d)
                .build();
        assertThat(AlertThresholds.isQueueWait(policy, metric(9, 100, 100, 0))).isFalse();
        assertThat(AlertThresholds.isQueueWait(policy, metric(10, 100, 100, 0))).isTrue();
    }

    @Test
    void queueWaitPercentOfCapacity() {
        AlertPolicy policy = AlertPolicy.builder()
                .queueMetric(QueueAlertMetric.WAITING)
                .queueCompare(QueueAlertCompare.PERCENT)
                .queueThreshold(50d)
                .build();
        assertThat(AlertThresholds.isQueueWait(policy, metric(49, 0, 100, 0))).isFalse();
        assertThat(AlertThresholds.isQueueWait(policy, metric(50, 0, 100, 0))).isTrue();
        assertThat(AlertThresholds.isQueueWait(policy, metric(50, 0, 0, 0))).isFalse();
    }

    @Test
    void queueWaitAbsoluteActive() {
        AlertPolicy policy = AlertPolicy.builder()
                .queueMetric(QueueAlertMetric.ACTIVE)
                .queueCompare(QueueAlertCompare.COUNT)
                .queueThreshold(5d)
                .build();
        assertThat(AlertThresholds.isQueueWait(policy, metric(100, 4, 100, 0))).isFalse();
        assertThat(AlertThresholds.isQueueWait(policy, metric(100, 5, 100, 0))).isTrue();
    }

    @Test
    void surgeUsesEstimatedWaitTime() {
        AlertPolicy policy = AlertPolicy.builder().surgeWaitTimeSeconds(60).build();
        assertThat(AlertThresholds.isWaitTimeExceeded(policy, metric(0, 0, 100, 59))).isFalse();
        assertThat(AlertThresholds.isWaitTimeExceeded(policy, metric(0, 0, 100, 60))).isTrue();
        assertThat(AlertThresholds.isWaitTimeExceeded(policy, metric(0, 0, 100, -1))).isTrue();
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
