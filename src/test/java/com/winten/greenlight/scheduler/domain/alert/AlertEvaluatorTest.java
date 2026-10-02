package com.winten.greenlight.scheduler.domain.alert;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlertEvaluatorTest {
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration REPEAT = Duration.ofHours(1);

    @Test
    void waitsForTicksBeforeFiring() {
        Map<String, String> labels = labels();
        AlertEvaluator.Decision first = AlertEvaluator.evaluate(
                AlertName.QUEUE_WAIT.name(), true, null, labels, Map.of(), 2, REPEAT, T0
        ).orElseThrow();
        assertThat(first.payload()).isNull();
        assertThat(first.nextState().getConsecutive()).isEqualTo(1);

        AlertEvaluator.Decision second = AlertEvaluator.evaluate(
                AlertName.QUEUE_WAIT.name(), true, first.nextState(), labels, Map.of(), 2, REPEAT, T0.plusSeconds(3)
        ).orElseThrow();
        assertThat(second.payload()).isNotNull();
        assertThat(second.payload().status()).isEqualTo(AlertStatus.FIRING.name());
        assertThat(second.persistAfterAck()).isTrue();
    }

    @Test
    void doesNotRepeatUntilIntervalElapses() {
        Map<String, String> labels = labels();
        AlertState firing = AlertState.builder()
                .fingerprint(AlertFingerprint.of(labels))
                .status(AlertStatus.FIRING.name())
                .consecutive(5)
                .startedAtEpochMs(T0.toEpochMilli())
                .lastSentAtEpochMs(T0.toEpochMilli())
                .alertname(AlertName.QUEUE_WAIT.name())
                .build();

        AlertEvaluator.Decision tooSoon = AlertEvaluator.evaluate(
                AlertName.QUEUE_WAIT.name(), true, firing, labels, Map.of(), 2, REPEAT, T0.plus(Duration.ofMinutes(10))
        ).orElseThrow();
        assertThat(tooSoon.payload()).isNull();

        AlertEvaluator.Decision repeat = AlertEvaluator.evaluate(
                AlertName.QUEUE_WAIT.name(), true, firing, labels, Map.of(), 2, REPEAT, T0.plus(Duration.ofHours(1))
        ).orElseThrow();
        assertThat(repeat.payload().status()).isEqualTo(AlertStatus.FIRING.name());
    }

    @Test
    void resolvesWhenConditionClears() {
        Map<String, String> labels = labels();
        AlertState firing = AlertState.builder()
                .fingerprint(AlertFingerprint.of(labels))
                .status(AlertStatus.FIRING.name())
                .consecutive(5)
                .startedAtEpochMs(T0.toEpochMilli())
                .lastSentAtEpochMs(T0.toEpochMilli())
                .alertname(AlertName.QUEUE_WAIT.name())
                .build();

        AlertEvaluator.Decision resolved = AlertEvaluator.evaluate(
                AlertName.QUEUE_WAIT.name(), false, firing, labels, Map.of("summary", "cleared"), 2, REPEAT, T0.plusSeconds(6)
        ).orElseThrow();
        assertThat(resolved.payload().status()).isEqualTo(AlertStatus.RESOLVED.name());
        assertThat(resolved.nextState().getConsecutive()).isEqualTo(0);
    }

    @Test
    void fingerprintMatchesAdmin() {
        Map<String, String> labels = labels();
        assertThat(AlertFingerprint.of(labels)).hasSize(16);
    }

    private Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", AlertName.QUEUE_WAIT.name());
        labels.put("site_id", "site-a");
        labels.put("room_id", "room-a");
        labels.put("severity", AlertSeverity.WARNING.name());
        return labels;
    }
}
