package com.winten.greenlight.scheduler.db.repository.redis.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminPreferenceEntity implements Serializable {
    /**
     * 이 그룹에 속한 모든 Action에 걸쳐 허용되는 최대 동시 활성 사용자(또는 세션)의 수입니다.
     * 이 값을 초과하는 요청은 대기열로 보내집니다.
     */
    private Integer currentActiveCustomers;

    /**
     * 그룹 전체의 활성화 여부를 나타내는 플래그입니다.
     * {@code false}일 경우, 이 그룹에 속한 모든 Action의 대기열 기능이 비활성화됩니다.
     */
    private Boolean isGreenlightEnabled;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}