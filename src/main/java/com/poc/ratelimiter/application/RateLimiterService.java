
package com.poc.ratelimiter.application;

import com.poc.ratelimiter.config.ClientRateLimitConfig;
import com.poc.ratelimiter.domain.DistributedHighThroughputRateLimiter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RateLimiterService {

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
            System.out.println("Client " + clientId + " with limit " + limit + " allowed? " + isAllowed)
        );

        return allowed;
    }
}
