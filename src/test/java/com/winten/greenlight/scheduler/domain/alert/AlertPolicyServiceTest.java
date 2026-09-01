package com.winten.greenlight.scheduler.domain.alert;

import com.winten.greenlight.scheduler.db.repository.redis.alert.RedisAlertPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertPolicyServiceTest {
    @Mock private RedisAlertPolicyRepository alertPolicyRepository;

    @Test
    void missingCacheUsesDefaults() {
        when(alertPolicyRepository.find("site-a")).thenReturn(null);

        AlertPolicy policy = new AlertPolicyService(alertPolicyRepository).get("site-a");

        assertThat(policy.getForTicks()).isEqualTo(2);
        assertThat(policy.getQueueMetric()).isEqualTo(QueueAlertMetric.WAITING);
        assertThat(policy.getQueueThreshold()).isEqualTo(1d);
        assertThat(policy.getSurgeWaitTimeSeconds()).isEqualTo(60);
    }

    @Test
    void usableCachedPolicyIsReturned() {
        AlertPolicy cached = AlertPolicy.builder()
                .forTicks(4)
                .repeatIntervalSeconds(90)
                .queueMetric(QueueAlertMetric.ACTIVE)
                .queueCompare(QueueAlertCompare.PERCENT)
                .queueThreshold(25d)
                .surgeWaitTimeSeconds(15)
                .build();
        when(alertPolicyRepository.find("site-a")).thenReturn(cached);

        AlertPolicy policy = new AlertPolicyService(alertPolicyRepository).get("site-a");

        assertThat(policy.getForTicks()).isEqualTo(4);
        assertThat(policy.getQueueMetric()).isEqualTo(QueueAlertMetric.ACTIVE);
        assertThat(policy.getSurgeWaitTimeSeconds()).isEqualTo(15);
    }
}
