
package com.winten.greenlight.scheduler.domain.actiongroup.service;

import com.winten.greenlight.scheduler.db.repository.redis.actiongroup.repository.ActionGroupAccessLogRepository;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionGroupAccessLogService {
    private final ActionGroupAccessLogRepository actionGroupAccesslogRepository;
    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * action_group:{actionGroupId}:accesslog 의 {activeDurationSeconds} 을/를 초과한 활성 사용자 제거
     *
     * @param actionGroupId Action Group PK
     * @param activeDurationSeconds 해당 시각을 초과한 사용자를 제거하는 기준(초,sec)
     */
    public void removeOverExpireMinuteCustomersFromActionGroupAccessLogBy(Long actionGroupId, Integer activeDurationSeconds) {
        String key = redisKeyBuilder.actionGroupAccessLog(actionGroupId);
        if (activeDurationSeconds <= 0L) {
            throw new IllegalArgumentException("expireMinute must be positive.");
        }

        // 현재 시각 기준 N초 이전(ex: 1초 = 1000밀리초) 이전 timestamp 계산
        long currentTimeMillis = System.currentTimeMillis();
        long expireTime = currentTimeMillis - (activeDurationSeconds * 1000L);

        actionGroupAccesslogRepository.removeZset(key, expireTime);
    }

    /**
     * action_group:{actionGroupId}:session 의 {activeDurationSeconds} 을/를 초과한 활성 사용자 제거
     *
     * @param actionGroupId Action Group PK
     * @param activeDurationSeconds 해당 시각을 초과한 사용자를 제거하는 기준(초,sec)
     */
    public void removeExpiredActionGroupSession(Long actionGroupId, Integer activeDurationSeconds) {
        String key = redisKeyBuilder.actionGroupSession(actionGroupId);
        if (activeDurationSeconds <= 0L) {
            throw new IllegalArgumentException("expireMinute must be positive.");
        }

        // 현재 시각 기준 N초 이전(ex: 1초 = 1000밀리초) 이전 timestamp 계산
        long currentTimeMillis = System.currentTimeMillis();
        long expireTime = currentTimeMillis - (activeDurationSeconds * 1000L);

        actionGroupAccesslogRepository.removeZset(key, expireTime);
    }

    /**
     * {actionGroupId} 별 action_group:{actionGroupId}:accesslog 키의 활성 사용자 수(currentActiveCustomers) 집계
     *
     * @param actionGroupId Action Group PK
     * @return Integer Current Active Customers count 해당 Action Group ID 에 매핑되는 현재 활성 사용자 수
     */
    public Integer getCurrentActiveCustomersCountFromActionGroupAccessLogBy(Long actionGroupId) {
        String key = redisKeyBuilder.actionGroupAccessLog(actionGroupId);
        if (actionGroupId <= 0L) {
            throw new IllegalArgumentException("actionGroupId must be positive.");
        }

        return Math.toIntExact(actionGroupAccesslogRepository.getCurrentActiveCustomersCountFromActionGroupAccessLogBy(key));
    }
}