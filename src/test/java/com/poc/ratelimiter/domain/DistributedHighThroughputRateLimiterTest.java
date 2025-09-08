package com.poc.ratelimiter.domain;

import org.junit.jupiter.api.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DistributedHighThroughputRateLimiterTest {

    private DistributedKeyValueStore store;
    private DistributedHighThroughputRateLimiter limiter;

    @BeforeEach
    void setUp() {
        store = new InMemoryTestStore();
        
        limiter = new DistributedHighThroughputRateLimiter(store);
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
            assertTrue(limiter.isAllowed(client, limit).get(), "Request " + (i + 1) + " should be allowed.");
        }
        
        assertFalse(limiter.isAllowed(client, limit).get(), "The request exceeding the limit should be blocked.");
    }

    @Test
    void shouldHandleMultipleClientsIndependently() throws Exception {
        String clientA = "A";
        String clientB = "B";
        int limit = 5;

        for (int i = 0; i < limit; i++) {
            assertTrue(limiter.isAllowed(clientA, limit).get());
            assertTrue(limiter.isAllowed(clientB, limit).get());
        }

        assertFalse(limiter.isAllowed(clientA, limit).get());
        assertFalse(limiter.isAllowed(clientB, limit).get());
    }

    @Test
    void shouldNotThrowOnBackendFailure() throws Exception {
        DistributedKeyValueStore failingStore = (key, delta, exp) ->
                CompletableFuture.failedFuture(new RuntimeException("backend down"));
        DistributedHighThroughputRateLimiter faultyLimiter = new DistributedHighThroughputRateLimiter(failingStore);

        assertTrue(faultyLimiter.isAllowed("clientX", 10).get(), "Fallback should allow");
        faultyLimiter.shutdown();
    }

    @Test
    void shouldBatchRequestsAndFlush() throws Exception {
        String client = "batchClient";
        
        for (int i = 0; i < 50; i++) {
            limiter.isAllowed(client, 100).get();
        }
        
        Thread.sleep(300);

        
        int totalStoredCount = 0;
        int shardCount = 10; 
        for (int i = 0; i < shardCount; i++) {
            String shardKey = client + "#" + i;
            totalStoredCount += store.incrementByAndExpire(shardKey, 0, 60).get();
        }

        assertTrue(totalStoredCount >= 50, "Batch flush should update the distributed store. Found " + totalStoredCount);
    }

    
    static class InMemoryTestStore implements DistributedKeyValueStore {
        private final ConcurrentHashMap<String, Integer> storage = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Integer> incrementByAndExpire(String key, int delta, int expirationSeconds) {
            storage.merge(key, delta, Integer::sum);
            return CompletableFuture.completedFuture(storage.getOrDefault(key, 0));
        }
    }
}