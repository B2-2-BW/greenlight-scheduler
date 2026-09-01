package com.winten.greenlight.scheduler.domain.alert;

import com.winten.greenlight.scheduler.domain.room.RoomMetric;

public final class AlertThresholds {
    private AlertThresholds() {
    }

    public static boolean isQueueWait(AlertPolicy policy, RoomMetric metric) {
        if (policy == null || metric == null) {
            return false;
        }
        long value = policy.getQueueMetric() == QueueAlertMetric.ACTIVE
                ? metric.getTotalActive()
                : metric.getTotalWaiting();
        Double threshold = policy.getQueueThreshold();
        if (threshold == null || threshold <= 0) {
            return false;
        }
        if (policy.getQueueCompare() == QueueAlertCompare.PERCENT) {
            int capacity = metric.getRoomCapacity();
            if (capacity <= 0) {
                return false;
            }
            return value * 100.0d / capacity >= threshold;
        }
        return value >= threshold;
    }

    public static boolean isWaitTimeExceeded(AlertPolicy policy, RoomMetric metric) {
        if (policy == null || metric == null) {
            return false;
        }
        int limitSeconds = policy.getSurgeWaitTimeSeconds();
        if (limitSeconds < 1) {
            return false;
        }
        long estimatedWaitTime = metric.getEstimatedWaitTime();
        if (estimatedWaitTime < 0) {
            return true;
        }
        return estimatedWaitTime >= limitSeconds;
    }
}
