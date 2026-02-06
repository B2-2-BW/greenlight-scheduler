package com.winten.greenlight.scheduler.db.repository.redis.admin;

import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.scheduler.domain.admin.AdminPreference;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorCode;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@Slf4j
@RequiredArgsConstructor
public class AdminPreferenceRepository {
    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final JsonMapper jsonMapper;
    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * getAdminPreference 전체 액션그룹 메타(테이블) 정보 가져오기
     *
     * @return AdminPreferenceEntity
     */
    public AdminPreference getAdminPreference() {
        String key = redisKeyBuilder.adminPreference();
        try {
            Map<Object, Object> map = jsonRedisTemplate.opsForHash().entries(key);
            return jsonMapper.convertValue(map, AdminPreference.class);
        } catch (Exception e) {
            log.error("Redis 오류 발생: {}", e.getMessage(), e);
            throw CoreException.of(ErrorCode.REDIS_ERROR, "Redis 접근 오류: " + e.getMessage());
        }
    }
}