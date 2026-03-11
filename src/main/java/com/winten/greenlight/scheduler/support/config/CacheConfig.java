package com.winten.greenlight.scheduler.support.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("roomCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(100));
        return cacheManager;
    }
}