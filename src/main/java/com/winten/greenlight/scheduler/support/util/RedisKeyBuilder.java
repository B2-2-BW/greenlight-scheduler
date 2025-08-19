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
}