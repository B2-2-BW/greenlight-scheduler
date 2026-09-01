package com.winten.greenlight.scheduler.support.util;

import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisKeyBuilder {
    @Value("${redis.key-prefix}")
    private String prefix;

    public String allActionGroupMeta() {
        return String.format("%s:action_group:*:meta", prefix);
    }

    public String actionGroupRequestLog(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":request_log";
    }

    public String actionGroupAccessLog(Long actionGroupId) {
        return prefix + ":action_group:" + actionGroupId + ":access_log";
    }

    public String actionGroupQueue(Long actionGroupId, WaitStatus waitStatus) {
        return prefix + ":action_group:" + actionGroupId + ":queue:" + waitStatus;
    }

    public String adminPreference() {
        return prefix + ":admin:preference";
    }

    public String actionGroupSession() {
        return prefix + ":session";
    }

    public String actionEventStream() {
        return prefix + ":infra:action_event:stream";
    }

    public String actionGroupWaitStatusPattern() {
        return prefix + ":action_group:*:queue:*";
    }

    public String allRoomMeta()  {
        return prefix + ":room:{*}:meta";
    }

    public String roomQueue(String roomId, WaitStatus waitStatus) {
        return prefix + ":room:{" + roomId + "}:queue:" + waitStatus;
    }

    public String roomHeartbeat(String roomId, WaitStatus heartbeatType) {
        return prefix + ":room:{" + roomId + "}:heartbeat:" + heartbeatType;
    }
    public String roomMetricCounter(String roomId, WaitStatus waitStatus, long timestamp) {
        return prefix + ":room:{" + roomId + "}:metric:counter:" + waitStatus + ":" + timestamp;
    }
    public String roomMetricLatest(String roomId) {
        return prefix + ":room:{" + roomId + "}:metric:latest";
    }
    public String roomMetricVersion() {
        return prefix + ":room:versions:metric";
    }
    public String roomMetricExited5m(String roomId) {
        return prefix + ":room:{" + roomId + "}:metric:exited_5m";
    }

    public String roomMeta(String roomId) {
        return prefix + ":room:{" + roomId + "}:meta";
    }

    public String siteRoomId() {
        return prefix + ":site:roomId";
    }

    public String alert(String fingerprint) {
        return prefix + ":alert:" + fingerprint;
    }

    public String alertPattern() {
        return prefix + ":alert:*";
    }

    public String alertPolicy(String siteId) {
        return prefix + ":admin:alert_policy:" + siteId;
    }
}