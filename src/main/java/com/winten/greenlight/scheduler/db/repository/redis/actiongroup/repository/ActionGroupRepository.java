package com.winten.greenlight.scheduler.db.repository.redis.actiongroup.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.ActionGroupEntity;
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
public class ActionGroupRepository {
    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * getAllActionGroups 전체 액션그룹 메타(테이블) 정보 가져오기
     *
     * @return List<ActionGroupEntity>
     */
    public List<ActionGroupEntity> getAllActionGroupsMetaEntity(String allKey) {
        Set<String> keys = jsonRedisTemplate.keys(allKey);
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActionGroupEntity> result = new ArrayList<>();

        for(String key : keys){
            try {
                Map<Object, Object> map = jsonRedisTemplate.opsForHash().entries(key);
                ActionGroupEntity actionGroupEntity = objectMapper.convertValue(map, ActionGroupEntity.class);
                result.add(actionGroupEntity);
            } catch (Exception e) {
                log.error("Redis 오류 발생: {}", e.getMessage(), e);
                throw CoreException.of(ErrorType.REDIS_ERROR, "Redis 접근 오류: " + e.getMessage());
            }
        }

        return result;
    }
}
