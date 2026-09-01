package com.winten.greenlight.scheduler.db.repository.redis.alert;

import com.winten.greenlight.scheduler.domain.alert.AlertPolicy;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisAlertPolicyRepository {
    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;
    private final JsonMapper jsonMapper;

    public AlertPolicy find(String siteId) {
        if (siteId == null || siteId.isBlank()) {
            return null;
        }
        String key = redisKeyBuilder.alertPolicy(siteId);
        Map<Object, Object> values = jsonRedisTemplate.opsForHash().entries(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return jsonMapper.convertValue(values, AlertPolicy.class);
    }
}
