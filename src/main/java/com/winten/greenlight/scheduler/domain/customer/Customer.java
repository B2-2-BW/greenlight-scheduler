package com.winten.greenlight.scheduler.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    private Long actionId;
    private String customerId;
    private double score;
    private WaitStatus waitStatus;
}