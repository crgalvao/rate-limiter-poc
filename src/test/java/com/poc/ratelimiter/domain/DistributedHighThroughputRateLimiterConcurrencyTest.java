package com.poc.ratelimiter.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DistributedHighThroughputRateLimiterConcurrencyTest {

    private final TrackingInMemoryStore store = new TrackingInMemoryStore();
    private final DistributedHighThroughputRateLimiter limiter =
            new DistributedHighThroughputRateLimiter(store);

    @AfterEach
    void tearDown() {
        limiter.shutdown();
    }

    @Test
    void stress_manyThreads_hotKey_underAndOverLimitBounds() throws Exception {
        final String client = "stress-hot-key";
        final int totalRequests = 10_000;       
        final int threadCount   = 64;
        final int limit         = 5_000;        
        final int toleratedOverage = (int) (limit * 0.15); 

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate  = new CountDownLatch(totalRequests);

        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger failedFutures = new AtomicInteger();

        for (int i = 0; i < totalRequests; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    boolean ok = limiter.isAllowed(client, limit)
                            .orTimeout(2, TimeUnit.SECONDS)
                            .exceptionally(ex -> {
                                failedFutures.incrementAndGet();
                                return false;
                            }).join();
                    if (ok) allowed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        
        startGate.countDown();

        
        assertTrue(doneGate.await(15, TimeUnit.SECONDS), "Requests took too long to finish");
        pool.shutdownNow();

        
        Thread.sleep(400);

        int allowedCount = allowed.get();
        int failedCount = failedFutures.get();

        
        assertEquals(0, failedCount, "There were failed futures / backend errors");

        
        assertTrue(allowedCount >= limit,
                "Allowed less than the configured limit; allowed=" + allowedCount + " limit=" + limit);

        
        assertTrue(allowedCount <= limit + toleratedOverage,
                "Allowed far above limit; allowed=" + allowedCount + " limit=" + limit
                        + " toleratedOverage=" + toleratedOverage);

        
        

        
        
        
        int stored = 0;
        
        for (int i = 0; i < 10; i++) {
            stored += store.getValue(client + "#" + i);
        }

        assertTrue(stored >= Math.min(totalRequests, limit + toleratedOverage),
                "Distributed store didn't reflect expected batched increments. stored=" + stored);

        
        
        long writeCalls = store.getWriteCalls(client);
        assertTrue(writeCalls < totalRequests / 2,
                "Too many write calls to store (batching ineffective). writes=" + writeCalls);

    }

    static class TrackingInMemoryStore implements DistributedKeyValueStore {
        private final ConcurrentHashMap<String, AtomicInteger> storage = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicInteger> writeCalls = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicInteger> readCalls  = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Integer> incrementByAndExpire(String key, int delta, int expirationSeconds) {
            if (delta == 0) {
                readCalls.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
                int val = storage.getOrDefault(key, new AtomicInteger(0)).get();
                return CompletableFuture.completedFuture(val);
            } else {
                writeCalls.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
                storage.computeIfAbsent(key, k -> new AtomicInteger()).addAndGet(delta);
                return CompletableFuture.completedFuture(storage.get(key).get());
            }
        }

        int getValue(String key) {
            return storage.getOrDefault(key, new AtomicInteger(0)).get();
        }

        int getWriteCalls(String key) {
            return writeCalls.getOrDefault(key, new AtomicInteger(0)).get();
        }

        int getReadCalls(String key) {
            return readCalls.getOrDefault(key, new AtomicInteger(0)).get();
        }
    }
}
