
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
     */
    public void saveActionGroupStatusBy(ActionGroupStatus status) {
        if (status.getId() <= 0L) {
            throw new IllegalArgumentException("actionGroupId must be positive.");
        }

        String key = redisKeyBuilder.actionGroupStatus(status.getId());
        Map<Object, Object> statusMap = new HashMap<>();
        statusMap.put("id", status.getId());
        statusMap.put("currentActiveCustomers", status.getCurrentActiveCustomers());
        statusMap.put("availableCapacity", status.getAvailableCapacity());
        statusMap.put("waitingQueueCount", status.getWaitingQueueCount());

        //JSON 문자열로 직렬화
        //String jsonString = objectMapper.convertValue(statusMap, String.class);

        actionGroupStatusRepository.saveActionGroupStatusBy(key, statusMap);
    }
}