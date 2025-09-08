package com.poc.ratelimiter.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class DistributedHighThroughputRateLimiter {

    private static final int WINDOW_SECONDS = 60;
    private static final int FLUSH_INTERVAL_MS = 100;
    private static final int SHARD_COUNT = 10; 

    private final DistributedKeyValueStore store;
    private final Map<String, LongAdder> localCounters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public DistributedHighThroughputRateLimiter(DistributedKeyValueStore store) {
        this.store = store;
        startBackgroundFlush();
    }

    public CompletableFuture<Boolean> isAllowed(String key, int limit) {
        final LongAdder localCounter = localCounters.computeIfAbsent(key, k -> new LongAdder());
        localCounter.increment();

        
        List<CompletableFuture<Integer>> shardFutures = new ArrayList<>();
        for (int i = 0; i < SHARD_COUNT; i++) {
            String shardKey = getShardKey(key, i);
            shardFutures.add(store.incrementByAndExpire(shardKey, 0, WINDOW_SECONDS));
        }

        
        return CompletableFuture.allOf(shardFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int totalCount = shardFutures.stream()
                            .mapToInt(CompletableFuture::join)
                            .sum();
                    
                    return totalCount + localCounter.sum() <= limit;
                })
                .exceptionally(ex -> {
                    System.err.println("Erro ao verificar limite para " + key + ": " + ex);
                    return true; 
                });
    }

    private void startBackgroundFlush() {
        scheduler.scheduleAtFixedRate(this::flushLocalCounters,
                FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void flushLocalCounters() {
        for (Map.Entry<String, LongAdder> entry : localCounters.entrySet()) {
            String key = entry.getKey();
            LongAdder adder = entry.getValue();
            long delta = adder.sumThenReset();

            if (delta > 0) {
                
                int shardId = random.nextInt(SHARD_COUNT);
                String shardKey = getShardKey(key, shardId);

                store.incrementByAndExpire(shardKey, (int) delta, WINDOW_SECONDS)
                        .exceptionally(ex -> {
                            System.err.println("Falha no flush para " + shardKey + ": " + ex);
                            
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
            
            if (!scheduler.awaitTermination(FLUSH_INTERVAL_MS * 2L, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}