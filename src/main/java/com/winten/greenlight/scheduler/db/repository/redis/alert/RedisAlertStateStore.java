package com.winten.greenlight.scheduler.db.repository.redis.alert;

import com.winten.greenlight.scheduler.domain.alert.AlertState;
import com.winten.greenlight.scheduler.domain.alert.AlertStateStore;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisAlertStateStore implements AlertStateStore {
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;
    private final JsonMapper jsonMapper;

    @Override
    public AlertState get(String fingerprint) {
        String json = redisTemplate.opsForValue().get(redisKeyBuilder.alert(fingerprint));
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readValue(json, AlertState.class);
        } catch (Exception exception) {
            log.warn("Failed to parse alert state. fingerprint={}", fingerprint, exception);
            return null;
        }
    }

    @Override
    public void put(String fingerprint, AlertState state) {
        try {
            redisTemplate.opsForValue().set(
                    redisKeyBuilder.alert(fingerprint),
                    jsonMapper.writeValueAsString(state),
                    TTL
            );
        } catch (Exception exception) {
            log.warn("Failed to save alert state. fingerprint={}", fingerprint, exception);
        }
    }

    @Override
    public void delete(String fingerprint) {
        redisTemplate.delete(redisKeyBuilder.alert(fingerprint));
    }

    @Override
    public List<AlertState> findAll() {
        List<AlertState> states = new ArrayList<>();
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(redisKeyBuilder.alertPattern())
                .count(100)
                .build();
        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> connection.keyCommands().scan(scanOptions)
        )) {
            if (cursor == null) {
                return states;
            }
            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                String json = redisTemplate.opsForValue().get(key);
                if (json == null || json.isBlank()) {
                    continue;
                }
                try {
                    states.add(jsonMapper.readValue(json, AlertState.class));
                } catch (Exception exception) {
                    log.warn("Failed to parse alert state. key={}", key, exception);
                }
            }
        }
        return states;
    }
}
