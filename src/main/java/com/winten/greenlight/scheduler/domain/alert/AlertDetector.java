package com.winten.greenlight.scheduler.domain.alert;

import com.winten.greenlight.scheduler.client.AdminAlertClient;
import com.winten.greenlight.scheduler.db.repository.redis.room.RoomRepository;
import com.winten.greenlight.scheduler.domain.room.Room;
import com.winten.greenlight.scheduler.domain.room.RoomEnvironment;
import com.winten.greenlight.scheduler.domain.room.RoomMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertDetector {
    private final AlertStateStore alertStateStore;
    private final AdminAlertClient adminAlertClient;
    private final AlertPolicyService alertPolicyService;
    private final RoomRepository roomRepository;

    public void evaluate(List<Room> rooms, List<RoomMetric> metrics) {
        Instant now = Instant.now();
        List<Pending> pending = new ArrayList<>();
        Set<String> seenRoomIds = new HashSet<>();
        Map<String, AlertPolicy> policies = new LinkedHashMap<>();
        Map<String, String> siteNames = new LinkedHashMap<>();

        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            RoomMetric metric = metrics.get(i);
            if (!Boolean.TRUE.equals(room.getEnabled()) || room.getRoomEnvironment() != RoomEnvironment.LIVE) {
                continue;
            }
            seenRoomIds.add(room.getRoomId());
            AlertPolicy policy = policyFor(policies, room.getSiteId());
            int forTicks = policy.getForTicks();
            Duration repeatInterval = Duration.ofSeconds(policy.getRepeatIntervalSeconds());
            String siteName = siteNames.computeIfAbsent(room.getSiteId(), this::siteName);
            pending.addAll(evaluateRule(
                    AlertName.QUEUE_WAIT,
                    AlertThresholds.isWaitingExceeded(policy, metric),
                    room,
                    annotations(room, siteName, metric, now),
                    now,
                    forTicks,
                    repeatInterval
            ));
        }

        for (AlertState state : alertStateStore.findAll()) {
            if (state.getRoomId() == null || seenRoomIds.contains(state.getRoomId())) {
                continue;
            }
            if (!AlertStatus.FIRING.name().equals(state.getStatus())) {
                continue;
            }
            AlertPolicy policy = policyFor(policies, state.getSiteId());
            Map<String, String> labels = labels(state.getAlertname(), state.getSiteId(), state.getRoomId());
            String siteName = siteNames.computeIfAbsent(state.getSiteId(), this::siteName);
            Map<String, String> annotations = new LinkedHashMap<>();
            annotations.put("summary", "대기 해소");
            annotations.put("description", "사이트: " + displayName(siteName, state.getSiteId())
                    + "\n대기열: " + displayName(null, state.getRoomId()));
            annotations.put("occurred_at", now.toString());
            AlertEvaluator.evaluate(
                    state.getAlertname(),
                    false,
                    state,
                    labels,
                    annotations,
                    policy.getForTicks(),
                    Duration.ofSeconds(policy.getRepeatIntervalSeconds()),
                    now
            ).filter(decision -> decision.payload() != null)
                    .ifPresent(decision -> pending.add(new Pending(decision, state)));
        }

        flush(pending, now);
    }

    private List<Pending> evaluateRule(
            AlertName alertName,
            boolean condition,
            Room room,
            Map<String, String> annotations,
            Instant now,
            int forTicks,
            Duration repeatInterval
    ) {
        String alertname = alertName.name();
        Map<String, String> labels = labels(alertname, room.getSiteId(), room.getRoomId());
        String fingerprint = AlertFingerprint.of(labels);
        AlertState previous = alertStateStore.get(fingerprint);
        return AlertEvaluator.evaluate(
                alertname, condition, previous, labels, annotations, forTicks, repeatInterval, now
        ).map(decision -> {
            if (decision.payload() == null) {
                AlertState next = decision.nextState();
                if (next.getConsecutive() > 0 || AlertStatus.FIRING.name().equals(next.getStatus())) {
                    alertStateStore.put(fingerprint, next);
                } else if (previous != null) {
                    alertStateStore.delete(fingerprint);
                }
                return null;
            }
            if (AlertStatus.RESOLVED.name().equals(decision.payload().status())) {
                decision.payload().annotations().put("summary", "대기 해소");
            }
            return new Pending(decision, previous);
        }).stream().filter(item -> item != null).toList();
    }

    private void flush(List<Pending> pending, Instant now) {
        if (pending.isEmpty()) {
            return;
        }
        List<AlertPayload> payloads = pending.stream().map(item -> item.decision.payload()).toList();
        boolean acked = adminAlertClient.sendAlerts(payloads);
        for (Pending item : pending) {
            AlertPayload payload = item.decision.payload();
            AlertState next = item.decision.nextState();
            if (acked) {
                AlertEvaluator.markAcked(next, payload, now);
                if (AlertStatus.RESOLVED.name().equals(payload.status())) {
                    alertStateStore.delete(next.getFingerprint());
                } else {
                    alertStateStore.put(next.getFingerprint(), next);
                }
            } else if (AlertStatus.FIRING.name().equals(payload.status())
                    && (item.previous == null || !AlertStatus.FIRING.name().equals(item.previous.getStatus()))) {
                alertStateStore.put(next.getFingerprint(), next);
            }
        }
    }

    private AlertPolicy policyFor(Map<String, AlertPolicy> policies, String siteId) {
        String key = siteId == null || siteId.isBlank() ? "" : siteId;
        return policies.computeIfAbsent(key, alertPolicyService::get);
    }

    private Map<String, String> labels(String alertname, String siteId, String roomId) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", alertname);
        if (siteId != null && !siteId.isBlank()) {
            labels.put("site_id", siteId);
        }
        if (roomId != null && !roomId.isBlank()) {
            labels.put("room_id", roomId);
        }
        labels.put("severity", AlertSeverity.WARNING.name());
        return labels;
    }

    private Map<String, String> annotations(Room room, String siteName, RoomMetric metric, Instant now) {
        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("summary", "대기 발생");
        annotations.put("description", "사이트: " + displayName(siteName, room.getSiteId())
                + "\n대기열: " + displayName(room.getName(), room.getRoomId())
                + "\n대기 " + metric.getTotalWaiting() + "명, 체류 " + metric.getTotalActive() + "명, 예상 대기시간 " + metric.getEstimatedWaitTime() + "초");
        annotations.put("occurred_at", now.toString());
        return annotations;
    }

    private String siteName(String siteId) {
        try {
            return roomRepository.findSiteName(siteId);
        } catch (Exception exception) {
            log.warn("site name redis read failed. siteId={}", siteId, exception);
            return null;
        }
    }

    static String displayName(String name, String id) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return id == null ? "" : id;
    }

    private record Pending(AlertEvaluator.Decision decision, AlertState previous) {
    }
}
