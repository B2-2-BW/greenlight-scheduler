package com.winten.greenlight.scheduler.domain.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SchedulerAlertClient {
    private final String adminApiUrl;
    private final String webhookToken;
    private final RestClient restClient = RestClient.builder().build();

    public SchedulerAlertClient(
            @Value("${admin.api.url:}") String adminApiUrl,
            @Value("${admin.alert.webhook.token:}") String webhookToken
    ) {
        this.adminApiUrl = adminApiUrl;
        this.webhookToken = webhookToken;
    }

    public void send(String alertname, String schedulerCode, boolean firing, String summary, String description) {
        if (adminApiUrl == null || adminApiUrl.isBlank() || webhookToken == null || webhookToken.isBlank()) {
            log.warn("Scheduler alert skipped: admin webhook is not configured");
            return;
        }
        String occurredAt = Instant.now().toString();
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", alertname);
        labels.put("scheduler_code", schedulerCode);
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("summary", summary);
        if (description != null && !description.isBlank()) {
            annotations.put("description", description);
        }
        annotations.put("occurred_at", occurredAt);
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("status", firing ? "FIRING" : "RESOLVED");
        alert.put("labels", labels);
        alert.put("annotations", annotations);
        if (firing) {
            alert.put("startsAt", occurredAt);
        } else {
            alert.put("endsAt", occurredAt);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("createdBy", "greenlight-scheduler");
        body.put("alerts", List.of(alert));
        try {
            restClient.post()
                    .uri(adminApiUrl.replaceAll("/+$", "") + "/alerts/general/webhook")
                    .header("X-ALERT-TOKEN", webhookToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Scheduler alert webhook failed. alertname={} schedulerCode={}", alertname, schedulerCode, exception);
        }
    }
}
