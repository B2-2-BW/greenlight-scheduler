package com.winten.greenlight.scheduler.client;

import com.winten.greenlight.scheduler.domain.alert.AlertFingerprint;
import com.winten.greenlight.scheduler.domain.alert.AlertName;
import com.winten.greenlight.scheduler.domain.alert.AlertPayload;
import com.winten.greenlight.scheduler.domain.alert.AlertSeverity;
import com.winten.greenlight.scheduler.domain.alert.AlertStatus;
import com.winten.greenlight.scheduler.domain.alert.AlertWebhookRequest;
import com.winten.greenlight.scheduler.domain.scheduler.SchedulerCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AdminAlertClient {

    private static final String ALERT_PATH = "/alerts/general/webhook";
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient adminApiRestClient;

    @Value("${alertmanager.token}")
    private String alertmanagerToken;

    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${spring.profiles.active}")
    private String profile;

    public AdminAlertClient(@Qualifier("adminApiRestClient") RestClient adminApiRestClient) {
        this.adminApiRestClient = adminApiRestClient;
    }

    public void sendAlert(
            AlertName alertname,
            AlertStatus status,
            SchedulerCode schedulerCode,
            String summary,
            String message
    ) {
        AlertStatus normalizedStatus = status == null ? AlertStatus.FIRING : status;
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", alertname.name());
        if (schedulerCode != null) {
            labels.put("scheduler_code", schedulerCode.name());
        }
        labels.put("severity", AlertSeverity.CRITICAL.name());
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("summary", summary);
        annotations.put("description", message);
        String occurredAt = Instant.now().toString();
        annotations.put("occurred_at", occurredAt);
        AlertPayload payload = new AlertPayload(
                normalizedStatus.name(),
                labels,
                annotations,
                occurredAt,
                normalizedStatus == AlertStatus.RESOLVED ? occurredAt : null,
                AlertFingerprint.of(labels)
        );
        sendAlertsWithRetry(List.of(payload));
    }

    public boolean sendAlerts(List<AlertPayload> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return true;
        }
        try {
            post(alerts);
            return true;
        } catch (Exception exception) {
            log.error("Admin alert batch send failed. size={}", alerts.size(), exception);
            return false;
        }
    }

    private void sendAlertsWithRetry(List<AlertPayload> alerts) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                post(alerts);
                return;
            } catch (Exception exception) {
                lastException = exception;
                log.warn("Admin alert send failed. attempt={}/{}", attempt, MAX_ATTEMPTS, exception);
                if (attempt < MAX_ATTEMPTS) {
                    sleepBeforeRetry(attempt);
                }
            }
        }
        log.error("Admin alert send failed after {} attempts.", MAX_ATTEMPTS, lastException);
    }

    private void post(List<AlertPayload> alerts) {
        adminApiRestClient.post()
                .uri(ALERT_PATH)
                .header("X-Alert-Token", alertmanagerToken)
                .body(new AlertWebhookRequest(createdBy(), alerts))
                .retrieve()
                .toBodilessEntity();
    }

    private String createdBy() {
        return applicationName + "-" + profile;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(1000L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Alert retry interrupted", e);
        }
    }
}
