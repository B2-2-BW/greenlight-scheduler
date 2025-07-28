package com.winten.greenlight.scheduler.db.repository.redis.actiongroup.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.ActionGroupStatusEntity;
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
public class ActionGroupStatusRepository {
    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * getAllActionGroups 전체 액션그룹 스테이터스(테이블) 정보 가져오기
     *
     * @return List<ActionGroupStatusEntity>
     */
    public List<ActionGroupStatusEntity> getAllActionGroupsStatusEntityBy(String allKey) {
        Set<String> keys = jsonRedisTemplate.keys(allKey);
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<ActionGroupStatusEntity> result = new ArrayList<>();

        for(String key : keys){
            try {
                Map<Object, Object> map = jsonRedisTemplate.opsForHash().entries(key);
                ActionGroupStatusEntity actionGroupStatusEntity = objectMapper.convertValue(map, ActionGroupStatusEntity.class);
                result.add(actionGroupStatusEntity);
            } catch (Exception e) {
                log.error("Redis 오류 발생: {}", e.getMessage(), e);
                throw CoreException.of(ErrorType.REDIS_ERROR, "Redis 접근 오류: " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * {actionGroupId} 별 action_group:{actionGroupId}:status 키의 {currentActiveCustomers}, {availableCapacity} 값 저장
     *
     * @param key String action_group:{actionGroupId}:status
     * @param actionGroupStatusMap {currentActiveCustomers}, {availableCapacity} 값
     */
    public void saveActionGroupStatusBy(String key, Map<Object, Object> actionGroupStatusMap) {
        jsonRedisTemplate.opsForHash().putAll(key, actionGroupStatusMap);
    }
}
