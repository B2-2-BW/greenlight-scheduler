package com.winten.greenlight.scheduler.domain.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertPolicy {
    private int forTicks;
    private int repeatIntervalSeconds;
    private QueueAlertMetric queueMetric;
    private QueueAlertCompare queueCompare;
    private Double queueThreshold;
    private int surgeWaitTimeSeconds;

    public static AlertPolicy defaults() {
        return AlertPolicy.builder()
                .forTicks(2)
                .repeatIntervalSeconds(3600)
                .queueMetric(QueueAlertMetric.WAITING)
                .queueCompare(QueueAlertCompare.COUNT)
                .queueThreshold(1d)
                .surgeWaitTimeSeconds(60)
                .build();
    }

    public boolean isUsable() {
        return forTicks >= 1
                && repeatIntervalSeconds >= 1
                && queueMetric != null
                && queueCompare != null
                && queueThreshold != null
                && queueThreshold > 0
                && surgeWaitTimeSeconds >= 1;
    }
}
