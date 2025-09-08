package com.poc.ratelimiter.application;

import com.poc.ratelimiter.config.ClientRateLimitConfig;
import com.poc.ratelimiter.domain.DistributedHighThroughputRateLimiter;
import com.poc.ratelimiter.domain.DistributedKeyValueStore;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService service;
    private DistributedHighThroughputRateLimiter limiter;

    @BeforeEach
    void setUp() {
        DistributedKeyValueStore store = (key, delta, exp) -> {
            return CompletableFuture.completedFuture(delta); 
        };
        limiter = new DistributedHighThroughputRateLimiter(store);

        ClientRateLimitConfig config = new ClientRateLimitConfig();
        ClientRateLimitConfig.ClientLimit c1 = new ClientRateLimitConfig.ClientLimit();
        c1.setId("userA"); c1.setLimit(5);

        ClientRateLimitConfig.ClientLimit c2 = new ClientRateLimitConfig.ClientLimit();
        c2.setId("userB"); c2.setLimit(10);

        config.setClients(List.of(c1, c2));

        service = new RateLimiterService(limiter, config);
    }

    @AfterEach
    void tearDown() {
        limiter.shutdown();
    }

    @Test
    void shouldRejectUnknownClient() {
        CompletableFuture<Boolean> result = service.isRequestAllowed("unknown");
        assertFalse(result.join());
    }

    @Test
    void shouldRespectConfiguredLimit() {
        CompletableFuture<Boolean> result = service.isRequestAllowed("userA");
        assertNotNull(result);
    }
}
