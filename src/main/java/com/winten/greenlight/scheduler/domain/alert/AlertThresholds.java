package com.winten.greenlight.scheduler.domain.alert;

import com.winten.greenlight.scheduler.domain.room.RoomMetric;

public final class AlertThresholds {
    private AlertThresholds() {
    }

    public static boolean isWaitingExceeded(AlertPolicy policy, RoomMetric metric) {
        if (policy == null || metric == null) {
            return false;
        }
        return isExceeded(policy.getWaitingCompare(), policy.getWaitingThreshold(), metric.getTotalWaiting(), metric.getRoomCapacity());
    }

    public static boolean isActiveExceeded(AlertPolicy policy, RoomMetric metric) {
        if (policy == null || metric == null) {
            return false;
        }
        return isExceeded(policy.getActiveCompare(), policy.getActiveThreshold(), metric.getTotalActive(), metric.getRoomCapacity());
    }

    private static boolean isExceeded(QueueAlertCompare compare, Double threshold, long value, int capacity) {
        if (threshold == null || threshold <= 0) {
            return false;
        }
        if (compare == QueueAlertCompare.PERCENT) {
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
        return metric.getEstimatedWaitTime() >= limitSeconds;
    }
}
