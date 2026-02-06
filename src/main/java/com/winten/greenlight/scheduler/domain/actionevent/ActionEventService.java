package com.winten.greenlight.scheduler.domain.actionevent;

import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionEventService {

    private final RedisKeyBuilder keyBuilder;
    private final RedisTemplate<String, String> redisTemplate;

    public void trimActionEventStream() {
        redisTemplate.opsForStream().trim(keyBuilder.actionEventStream(), 100_000);
    }
}