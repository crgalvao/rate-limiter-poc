package com.poc.ratelimiter.domain;

import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger; 

import static org.junit.jupiter.api.Assertions.*;

class DistributedHighThroughputRateLimiterTest {

    private DistributedKeyValueStore store;
    private DistributedHighThroughputRateLimiter limiter;

    @BeforeEach
    void setUp() {
        store = new InMemoryTestStore();
        
        
        limiter = new DistributedHighThroughputRateLimiter(store, 60, 100, 10);
    }

    @AfterEach
    void tearDown() {
        limiter.shutdown();
    }

    @Test
    void shouldAllowWhenBelowLimit() throws Exception {
        CompletableFuture<Boolean> result = limiter.isAllowed("client1", 5);
        assertTrue(result.get());
    }

    @Test
    void shouldBlockWhenAboveLimit() throws Exception {
        String client = "client2";
        int limit = 5;
        
        for (int i = 0; i < limit; i++) {
            
             limiter.isAllowed(client, limit).get();
        }
        
        assertFalse(limiter.isAllowed(client, limit).get(), "The request exceeding the limit should be blocked.");
    }

    static class InMemoryTestStore implements DistributedKeyValueStore {
        private final ConcurrentHashMap<String, AtomicInteger> storage = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Integer> incrementByAndExpire(String key, int delta, int expirationSeconds) {
            AtomicInteger value = storage.computeIfAbsent(key, k -> new AtomicInteger(0));
            if (delta > 0) {
                value.addAndGet(delta);
            }
            return CompletableFuture.completedFuture(value.get());
        }
    }
}