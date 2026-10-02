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
    private QueueAlertCompare waitingCompare;
    private Double waitingThreshold;
    private QueueAlertCompare activeCompare;
    private Double activeThreshold;
    private int surgeWaitTimeSeconds;

    public static AlertPolicy defaults() {
        return AlertPolicy.builder()
                .forTicks(2)
                .repeatIntervalSeconds(3600)
                .waitingCompare(QueueAlertCompare.COUNT)
                .waitingThreshold(1d)
                .activeCompare(QueueAlertCompare.COUNT)
                .activeThreshold(1d)
                .surgeWaitTimeSeconds(60)
                .build();
    }

    public boolean isUsable() {
        return forTicks >= 1
                && repeatIntervalSeconds >= 1
                && surgeWaitTimeSeconds >= 1
                && waitingCompare != null
                && waitingThreshold != null
                && waitingThreshold > 0
                && activeCompare != null
                && activeThreshold != null
                && activeThreshold > 0;
    }
}
