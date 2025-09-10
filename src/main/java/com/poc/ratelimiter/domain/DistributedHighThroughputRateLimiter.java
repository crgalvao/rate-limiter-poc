package com.poc.ratelimiter.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class DistributedHighThroughputRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(DistributedHighThroughputRateLimiter.class);

    private final int windowSeconds;
    private final int flushIntervalMs;
    private final int shardCount;
    private final DistributedKeyValueStore store;
    private final Map<String, LongAdder> localCounters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public DistributedHighThroughputRateLimiter(DistributedKeyValueStore store) {
        this(store, 60, 100, 10);
    }

    public DistributedHighThroughputRateLimiter(DistributedKeyValueStore store, int windowSeconds, int flushIntervalMs, int shardCount) {
        this.store = store;
        this.windowSeconds = windowSeconds;
        this.flushIntervalMs = flushIntervalMs;
        this.shardCount = shardCount;
        startBackgroundFlush();
    }

    public CompletableFuture<Boolean> isAllowed(String key, int limit) {
        final LongAdder localCounter = localCounters.computeIfAbsent(key, k -> new LongAdder());
        localCounter.increment();
        long currentLocalCount = localCounter.sum();

        List<CompletableFuture<Integer>> shardFutures = new ArrayList<>();
        for (int i = 0; i < shardCount; i++) {
            String shardKey = getShardKey(key, i);
            shardFutures.add(store.incrementByAndExpire(shardKey, 0, windowSeconds));
        }

        return CompletableFuture.allOf(shardFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int totalCount = shardFutures.stream()
                            .mapToInt(CompletableFuture::join)
                            .sum();
                    return totalCount + currentLocalCount <= limit;
                })
                .exceptionally(ex -> {
                    logger.error("Error checking rate limit for key {}: {}", key, ex.getMessage());
                    return true;
                });
    }


    private void startBackgroundFlush() {
        scheduler.scheduleAtFixedRate(this::flushLocalCounters,
                flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void flushLocalCounters() {
        for (Map.Entry<String, LongAdder> entry : localCounters.entrySet()) {
            String key = entry.getKey();
            LongAdder adder = entry.getValue();
            long delta = adder.sumThenReset();

            if (delta > 0) {
                int shardId = random.nextInt(shardCount);
                String shardKey = getShardKey(key, shardId);

                store.incrementByAndExpire(shardKey, (int) delta, windowSeconds)
                        .exceptionally(ex -> {
                            logger.error("Failed to flush for shardKey {}: {}", shardKey, ex.getMessage());
                            adder.add(delta);
                            return null;
                        });
            }
        }
    }

    private String getShardKey(String baseKey, int shardId) {
        return baseKey + "#" + shardId;
    }

    public void shutdown() {
        flushLocalCounters();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(flushIntervalMs * 2L, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}