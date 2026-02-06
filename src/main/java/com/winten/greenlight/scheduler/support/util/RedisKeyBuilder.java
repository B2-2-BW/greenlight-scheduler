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

    public String allActionGroupStatus() {
        return String.format("%s:action_group:*:status", prefix);
    }

    public String actionGroupStatus(Long actionGroupId) {
        return String.format("%s:action_group:%d:status", prefix, actionGroupId);
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
    public String actionEventDlqStream() {
        return prefix + ":infra:action_event:dlq";
    }

    public String actionGroupWaitStatusPattern() {
        return prefix + ":action_group:*:queue:*";
    }

    public String allRoomMeta()  {
        return prefix + ":room:*:meta";
    }

    public String roomQueue(String roomId, WaitStatus waitStatus) {
        return prefix + ":room:" + roomId + ":queue:" + waitStatus;
    }
}