package com.winten.greenlight.scheduler.domain.alert;

import java.util.List;

public record AlertWebhookRequest(
        String createdBy,
        List<AlertPayload> alerts
) {
}
