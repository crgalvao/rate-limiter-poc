package com.poc.ratelimiter.config;

import com.poc.ratelimiter.domain.DistributedHighThroughputRateLimiter;
import com.poc.ratelimiter.domain.DistributedKeyValueStore;
import com.poc.ratelimiter.infrastructure.InMemoryDistributedKeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public DistributedKeyValueStore distributedKeyValueStore() {
        return new InMemoryDistributedKeyValueStore();
    }

    @Bean
    public DistributedHighThroughputRateLimiter distributedHighThroughputRateLimiter(
            DistributedKeyValueStore store,
            RateLimiterProperties properties) {
        return new DistributedHighThroughputRateLimiter(
                store,
                properties.getWindowSeconds(),
                properties.getFlushIntervalMs(),
                properties.getShardCount()
        );
    }
}