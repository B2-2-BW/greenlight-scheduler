package com.winten.greenlight.scheduler.db.repository.redis.admin.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.ActionGroupEntity;
import com.winten.greenlight.scheduler.db.repository.redis.admin.AdminPreferenceEntity;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Slf4j
@RequiredArgsConstructor
public class AdminPreferenceRepository {
    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * getAdminPreference 전체 액션그룹 메타(테이블) 정보 가져오기
     *
     * @return AdminPreferenceEntity
     */
    public AdminPreferenceEntity getAdminPreferenceBy(String key) {
        AdminPreferenceEntity actionGroupEntity;

        try {
            Map<Object, Object> map = jsonRedisTemplate.opsForHash().entries(key);
            actionGroupEntity = objectMapper.convertValue(map, AdminPreferenceEntity.class);
        } catch (Exception e) {
            log.error("Redis 오류 발생: {}", e.getMessage(), e);
            throw CoreException.of(ErrorType.REDIS_ERROR, "Redis 접근 오류: " + e.getMessage());
        }

        return actionGroupEntity;
    }
}
