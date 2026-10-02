package com.winten.greenlight.scheduler.client;

import java.time.LocalDateTime;

public record GeneralMessage(
        String system,
        LocalDateTime sentAt,
        String alertType,
        String title,
        String message
) {
}