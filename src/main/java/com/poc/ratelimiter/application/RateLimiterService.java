package com.poc.ratelimiter.application;

import com.poc.ratelimiter.config.ClientRateLimitConfig;
import com.poc.ratelimiter.domain.DistributedHighThroughputRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);
    private final DistributedHighThroughputRateLimiter rateLimiter;
    private final ClientRateLimitConfig clientConfig;

    public RateLimiterService(DistributedHighThroughputRateLimiter rateLimiter,
                              ClientRateLimitConfig clientConfig) {
        this.rateLimiter = rateLimiter;
        this.clientConfig = clientConfig;
    }

    public CompletableFuture<Boolean> isRequestAllowed(String clientId) {
        Map<String, Integer> limits = clientConfig.getClientLimits();
        Integer limit = (limits != null) ? limits.get(clientId) : null;
        if (limit == null) {
            
            return CompletableFuture.completedFuture(false);
        }
        CompletableFuture<Boolean> allowed = rateLimiter.isAllowed(clientId, limit);

        
        
        allowed.thenAccept(isAllowed ->
            logger.info("Client {} with limit {} allowed? {}", clientId, limit, isAllowed)
        );

        return allowed;
    }
}