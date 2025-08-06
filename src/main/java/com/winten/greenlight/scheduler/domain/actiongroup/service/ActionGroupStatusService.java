
package com.winten.greenlight.scheduler.domain.actiongroup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.ActionGroupStatusEntity;
import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.repository.ActionGroupStatusRepository;
import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroupStatus;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionGroupStatusService {
    private final ActionGroupStatusRepository actionGroupStatusRepository;
    private final RedisKeyBuilder redisKeyBuilder;
    private final ObjectMapper objectMapper;

    public List<ActionGroupStatus> getAllActionGroupStatus() {
        String keys = redisKeyBuilder.allActionGroupStatus();

        //ActionGroupStatusEntity 리스트로 불러와준다.
        List<ActionGroupStatusEntity> actionGroupStatusEntities = actionGroupStatusRepository.getAllActionGroupsStatusEntityBy(keys);

        //ActionGroupStatusEntity 형식의 list 를 ActionGroupStatus 으로 매핑
        List<ActionGroupStatus> actionGroupStatuses = new ArrayList<>(actionGroupStatusEntities.size());
        for (ActionGroupStatusEntity actionGroupStatusEntity : actionGroupStatusEntities) {
            ActionGroupStatus actionGroupStatus = objectMapper.convertValue(actionGroupStatusEntity, ActionGroupStatus.class);
            actionGroupStatuses.add(actionGroupStatus);
        }
        return actionGroupStatuses;
    }

    /**
     * {actionGroupId} 별 action_group:{actionGroupId}:status 키의 {currentActiveCustomers}, {availableCapacity} 값 저장
     * 
     * @param actionGroupId          Action Group PK
     * @param currentActiveCustomers 저장 될 현재 활성 사용자 수
     * @param availableCapacity 계산된 유입 가능한 Capacity(케파)
     */
    public void saveActionGroupStatusBy(Long actionGroupId, Integer currentActiveCustomers, Integer availableCapacity) {
        String key = redisKeyBuilder.actionGroupStatus(actionGroupId);
        if (actionGroupId <= 0L) {
            throw new IllegalArgumentException("actionGroupId must be positive.");
        }

        Map<Object, Object> statusMap = new HashMap<>();
        statusMap.put("id", actionGroupId);
        statusMap.put("currentActiveCustomers", currentActiveCustomers);
        statusMap.put("availableCapacity", availableCapacity);

        //JSON 문자열로 직렬화
        //String jsonString = objectMapper.convertValue(statusMap, String.class);

        actionGroupStatusRepository.saveActionGroupStatusBy(key, statusMap);
    }
}