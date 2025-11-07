package com.winten.greenlight.scheduler.domain.actiongroup;

import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionGroupSizeMetric {
    private Long actionGroupId;
    private WaitStatus waitStatus;
    private String redisKey;
    private Long size;
}