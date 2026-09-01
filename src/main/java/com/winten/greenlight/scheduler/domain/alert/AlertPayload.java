package com.winten.greenlight.scheduler.domain.alert;

import java.util.Map;

public record AlertPayload(
        String status,
        Map<String, String> labels,
        Map<String, String> annotations,
        String startsAt,
        String endsAt,
        String fingerprint
) {
}
