package com.winten.greenlight.scheduler.db.repository.redis.actiongroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActionGroupStatusEntity implements Serializable {
    private Long id;
    private Long currentActiveCustomers;
    private Long availableCapacity;
}