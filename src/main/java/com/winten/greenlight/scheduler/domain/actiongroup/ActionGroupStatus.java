package com.winten.greenlight.scheduler.domain.actiongroup;

import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.ActionGroupStatusEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionGroupStatus implements Serializable {
    /**
     * ActionGroup의 고유 식별자(ID)입니다.
     */
    private Long id;

    /**
     * ActionGroup의 활성 사용자 수 입니다.
     */
    private Long currentActiveCustomers;

    /**
     * ActionGroup의 현재 접근 가능한 가용 인원 케파입니다.(접속 가능 여유 공간)
     */
    private Long availableCapacity;

    public ActionGroupStatus(final ActionGroupStatusEntity actionGroupStatusEntity) {
        this.id = actionGroupStatusEntity.getId();
        this.currentActiveCustomers = actionGroupStatusEntity.getCurrentActiveCustomers();
        this.availableCapacity = actionGroupStatusEntity.getAvailableCapacity();
    }
}