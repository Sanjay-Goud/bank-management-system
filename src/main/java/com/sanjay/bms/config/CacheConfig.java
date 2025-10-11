// src/main/java/com/sanjay/bms/config/CacheConfig.java
package com.sanjay.bms.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "userDashboard",
                "accountStatistics",
                "notifications",
                "transactions"
        );
    }
}