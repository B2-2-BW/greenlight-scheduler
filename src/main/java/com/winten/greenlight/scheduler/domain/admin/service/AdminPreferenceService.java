
package com.winten.greenlight.scheduler.domain.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.scheduler.db.repository.redis.admin.repository.AdminPreferenceRepository;
import com.winten.greenlight.scheduler.domain.admin.AdminPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPreferenceService {
    private final AdminPreferenceRepository adminPreferenceRepository;

    public AdminPreference getAdminPreference() {
        //AdminPreferenceEntity 를 불러와준다.
        return adminPreferenceRepository.getAdminPreference();
    }
}