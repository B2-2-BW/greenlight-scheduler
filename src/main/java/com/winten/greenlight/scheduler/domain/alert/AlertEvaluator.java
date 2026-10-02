package com.winten.greenlight.scheduler.domain.alert;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AlertEvaluator {
    private AlertEvaluator() {
    }

    public record Decision(AlertPayload payload, AlertState nextState, boolean persistAfterAck) {
    }

    public static Optional<Decision> evaluate(
            String alertname,
            boolean condition,
            AlertState previous,
            Map<String, String> labels,
            Map<String, String> annotations,
            int forTicks,
            Duration repeatInterval,
            Instant now
    ) {
        String fingerprint = AlertFingerprint.of(labels);
        long nowMs = now.toEpochMilli();
        AlertState current = previous == null ? empty(fingerprint, labels, alertname) : previous;

        if (condition) {
            int consecutive = current.getConsecutive() + 1;
            boolean firing = AlertStatus.FIRING.name().equals(current.getStatus());
            AlertState next = copy(current);
            next.setConsecutive(consecutive);
            next.setAlertname(alertname);
            next.setSiteId(labels.get("site_id"));
            next.setRoomId(labels.get("room_id"));

            if (!firing && consecutive >= forTicks) {
                next.setStartedAtEpochMs(nowMs);
                AlertPayload payload = payload(AlertStatus.FIRING, labels, annotations, now, null, fingerprint);
                return Optional.of(new Decision(payload, next, true));
            }
            if (firing && nowMs - current.getLastSentAtEpochMs() >= repeatInterval.toMillis()) {
                Instant startedAt = Instant.ofEpochMilli(
                        current.getStartedAtEpochMs() == 0 ? nowMs : current.getStartedAtEpochMs()
                );
                AlertPayload payload = payload(AlertStatus.FIRING, labels, annotations, startedAt, null, fingerprint);
                return Optional.of(new Decision(payload, next, true));
            }
            return Optional.of(new Decision(null, next, false));
        }

        AlertState next = copy(current);
        next.setConsecutive(0);
        if (AlertStatus.FIRING.name().equals(current.getStatus())) {
            Instant startedAt = Instant.ofEpochMilli(
                    current.getStartedAtEpochMs() == 0 ? nowMs : current.getStartedAtEpochMs()
            );
            AlertPayload payload = payload(AlertStatus.RESOLVED, labels, annotations, startedAt, now, fingerprint);
            return Optional.of(new Decision(payload, next, true));
        }
        return Optional.of(new Decision(null, next, false));
    }

    public static void markAcked(AlertState state, AlertPayload payload, Instant now) {
        if (payload == null || state == null) {
            return;
        }
        if (AlertStatus.FIRING.name().equals(payload.status())) {
            state.setStatus(AlertStatus.FIRING.name());
            state.setLastSentAtEpochMs(now.toEpochMilli());
            if (state.getStartedAtEpochMs() == 0) {
                state.setStartedAtEpochMs(now.toEpochMilli());
            }
        } else {
            state.setStatus(AlertStatus.RESOLVED.name());
            state.setLastSentAtEpochMs(0);
        }
    }

    private static AlertPayload payload(
            AlertStatus status,
            Map<String, String> labels,
            Map<String, String> annotations,
            Instant startsAt,
            Instant endsAt,
            String fingerprint
    ) {
        return new AlertPayload(
                status.name(),
                new LinkedHashMap<>(labels),
                annotations == null ? Map.of() : new LinkedHashMap<>(annotations),
                startsAt == null ? null : startsAt.toString(),
                endsAt == null ? null : endsAt.toString(),
                fingerprint
        );
    }

    private static AlertState empty(String fingerprint, Map<String, String> labels, String alertname) {
        return AlertState.builder()
                .fingerprint(fingerprint)
                .alertname(alertname)
                .status("")
                .consecutive(0)
                .siteId(labels.get("site_id"))
                .roomId(labels.get("room_id"))
                .build();
    }

    private static AlertState copy(AlertState state) {
        return AlertState.builder()
                .fingerprint(state.getFingerprint())
                .status(state.getStatus())
                .consecutive(state.getConsecutive())
                .startedAtEpochMs(state.getStartedAtEpochMs())
                .lastSentAtEpochMs(state.getLastSentAtEpochMs())
                .alertname(state.getAlertname())
                .siteId(state.getSiteId())
                .roomId(state.getRoomId())
                .build();
    }
}
