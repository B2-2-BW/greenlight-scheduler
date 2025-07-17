package com.winten.greenlight.scheduler.db.repository.redis.actiongroup.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ActionGroupAccessLogRepository {
    private final RedisTemplate<String, Object> jsonRedisTemplate;

    /**
     * action_group:{actionGroupId}:accesslog 의 {expireSeconds} 을/를 초과한 활성 사용자 제거
     *
     * @param key        Redis key
     * @param expireTime 삭제할 시각
     */
    public void removeOverExpireMinuteCustomersFromActionGroupAccessLogBy(String key, long expireTime) {
        Long count = jsonRedisTemplate.opsForZSet().removeRangeByScore(key, 0, expireTime);
        if (count != null && count > 0) {
            log.info("[removeOverExpireMinuteCustomersFromActionGroupAccessLogBy] {} expired users removed from {}.", count, key);
        } else {
            log.debug("[removeOverExpireMinuteCustomersFromActionGroupAccessLogBy] No expired users found for removal in {}.", key);
        }
    }

    /**
     * {actionGroupId} 별 action_group:{actionGroupId}:accesslog 키의 활성 사용자 수 (currentActiveCustomers) 집계
     *
     * @param key Redis key 여기선,
     * @return int currentActiveCustomers
     */
    public Long getCurrentActiveCustomersCountFromActionGroupAccessLogBy(String key) {
        Long count = jsonRedisTemplate.opsForZSet().count(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        if (count != null && count > 0L) {
            log.info("[getCurrentActiveCustomersCountFromActionGroupAccessLogBy] {} Active Customers from {}.", count, key);
        } else {
            log.debug("[getCurrentActiveCustomersCountFromActionGroupAccessLogBy] No Active Customers found in {}.", key);
        }

        return count;
    }
}
