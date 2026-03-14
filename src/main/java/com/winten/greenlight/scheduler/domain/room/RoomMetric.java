package com.winten.greenlight.scheduler.domain.room;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomMetric {
    private String roomId;
    private int roomCapacity;
    private long totalWaiting;
    private long totalActive;
    private long recentlyExited;
    private long waitingCount;
    private long enteredCount;
    private long exitedCount;
    private double waitingRate;
    private double enteredRate;
    private double exitedRate;
    private long estimatedWaitTime;
}