package com.winten.greenlight.scheduler.domain.admin;

import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.ActionGroupEntity;
import com.winten.greenlight.scheduler.support.dto.AuditDto;
import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminPreference extends AuditDto implements Serializable {

    /**
     * 활성 사용자로 간주하기 위한 최소 활동 시간(초)
     */
    private Integer activeCustomerDurationSeconds;

    /**
     * 그룹 전체의 활성화 여부를 나타내는 플래그입니다.
     * {@code false}일 경우, 이 그룹에 속한 모든 Action의 대기열 기능이 비활성화됩니다.
     */
    private Boolean isGreenlightEnabled;

    /**
     * 사용자 세션 유지시간 (동시접속자 수 계산 시 활용)
     */
    private Integer sessionDurationSeconds;

    public AdminPreference(final ActionGroupEntity actionGroupEntity) {
        this.createdBy = actionGroupEntity.getCreatedBy();
        this.createdAt = actionGroupEntity.getCreatedAt();
        this.updatedBy = actionGroupEntity.getUpdatedBy();
        this.updatedAt = actionGroupEntity.getUpdatedAt();
    }
}