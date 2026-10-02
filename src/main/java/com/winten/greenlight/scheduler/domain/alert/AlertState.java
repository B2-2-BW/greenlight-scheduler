package com.winten.greenlight.scheduler.domain.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertState {
    private String fingerprint;
    private String status;
    private int consecutive;
    private long startedAtEpochMs;
    private long lastSentAtEpochMs;
    private String alertname;
    private String siteId;
    private String roomId;
}
