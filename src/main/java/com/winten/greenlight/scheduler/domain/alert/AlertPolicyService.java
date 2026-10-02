package com.winten.greenlight.scheduler.domain.alert;

import com.winten.greenlight.scheduler.db.repository.redis.alert.RedisAlertPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertPolicyService {
    private final RedisAlertPolicyRepository alertPolicyRepository;

    public AlertPolicy get(String siteId) {
        try {
            AlertPolicy policy = alertPolicyRepository.find(siteId);
            if (policy != null && policy.isUsable()) {
                return policy;
            }
        } catch (Exception exception) {
            log.warn("alert_policy redis read failed, using defaults. siteId={}", siteId, exception);
        }
        return AlertPolicy.defaults();
    }
}
