
package com.winten.greenlight.scheduler.domain.actiongroup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.ActionGroupEntity;
import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.repository.ActionGroupRepository;
import com.winten.greenlight.scheduler.domain.actiongroup.ActionGroup;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionGroupService {
    private final ActionGroupRepository actionGroupRepository;
    private final RedisKeyBuilder redisKeyBuilder;
    private final ObjectMapper objectMapper;
    // TODO Layer 침범 리팩토링 필요
    private final RedisTemplate<String, String> redisTemplate;

    public List<ActionGroup> getAllActionGroupMeta() {
        String keys = redisKeyBuilder.allActionGroupMeta();

        //ActionGroupEntity 리스트로 불러와준다.
        List<ActionGroupEntity> actionGroupEntities = actionGroupRepository.getAllActionGroupsMetaEntityBy(keys);

        //ActionGroupEntity 형식의 list 를 ActionGroup 으로 매핑
        List<ActionGroup> actionGroups = new ArrayList<>(actionGroupEntities.size());
        for (ActionGroupEntity actionGroupEntity : actionGroupEntities) {
            ActionGroup actionGroup = objectMapper.convertValue(actionGroupEntity, ActionGroup.class);
            actionGroups.add(actionGroup);
        }

        return actionGroups;
    }

    public Long getWaitingQueueCountByActionGroupId(Long actionGroupId) {
        Long queueCount = actionGroupRepository.getWaitingQueueCount(actionGroupId);
        if (queueCount == null) {
            queueCount = 0L;
        }
        return queueCount;
    }

    public List<Float> getAllAccessPerSecOrdered(List<ActionGroup> actionGroupList) {
        List<Object> activeUserCounts = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (ActionGroup actionGroup : actionGroupList) {
                String key = redisKeyBuilder.actionGroupAccessLog(actionGroup.getId());
                connection.zSetCommands().zCard(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
        return activeUserCounts.stream().map(obj -> Float.parseFloat(obj.toString()) / 10).collect(Collectors.toList());
    }
}