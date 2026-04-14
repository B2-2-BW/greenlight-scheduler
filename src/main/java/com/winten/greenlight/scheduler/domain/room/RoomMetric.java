package com.winten.greenlight.scheduler.domain.room;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomMetric {
    private String roomId; // 대기열 ID
    private int roomCapacity; // 대기열 최대 수용인원
    private long totalWaiting; // 현재 총 대기인원
    private long totalActive; // 현재 총 활성사용자
    private long recentlyExited; // 3분동안 화면을 이탈한 전체 사용자 수
    private long waitingCount; // 3초동안 증가한 대기인원 수
    private long enteredCount; // 3초동안 화면에 진입한 인원 수
    private long exitedCount; // 3초동안 화면에서 이탈한 사용자 수
    private double waitingRate;  // 1초동안 증가한 대기인원 수 (count / 3)
    private double enteredRate; // 1초동안 화면에 진입한 인원 수 (count / 3)
    private double exitedRate; // 1초동안 화면에서 이탈한 사용자 수 (count / 3)
    private long estimatedWaitTime; // 예상 대기시간
}