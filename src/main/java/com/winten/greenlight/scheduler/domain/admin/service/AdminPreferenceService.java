
package com.winten.greenlight.scheduler.domain.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.scheduler.db.repository.redis.admin.AdminPreferenceEntity;
import com.winten.greenlight.scheduler.db.repository.redis.admin.repository.AdminPreferenceRepository;
import com.winten.greenlight.scheduler.domain.admin.AdminPreference;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPreferenceService {
    private final AdminPreferenceRepository adminPreferenceRepository;
    private final RedisKeyBuilder redisKeyBuilder;
    private final ObjectMapper objectMapper;

    public AdminPreference getAdminPreference() {
        String key = redisKeyBuilder.adminPreference();
        //AdminPreferenceEntity 를 불러와준다.
        AdminPreferenceEntity adminPreferenceEntity = adminPreferenceRepository.getAdminPreferenceBy(key);

        return objectMapper.convertValue(adminPreferenceEntity, AdminPreference.class);
    }
}