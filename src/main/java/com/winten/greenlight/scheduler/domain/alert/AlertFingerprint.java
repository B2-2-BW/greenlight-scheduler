package com.winten.greenlight.scheduler.domain.alert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

public final class AlertFingerprint {
    private AlertFingerprint() {
    }

    public static String of(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            throw new IllegalArgumentException("alert labels are required");
        }
        String canonical = labels.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
        if (canonical.isEmpty()) {
            throw new IllegalArgumentException("alert labels are required");
        }
        return sha256Hex(canonical).substring(0, 16);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
