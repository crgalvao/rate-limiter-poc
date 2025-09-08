package com.poc.ratelimiter.infrastructure;

import com.poc.ratelimiter.domain.DistributedKeyValueStore;

import java.util.Map;
import java.util.concurrent.*;

public class InMemoryDistributedKeyValueStore implements DistributedKeyValueStore {

    private final Map<String, Integer> storage = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Integer> incrementByAndExpire(String key, int delta, int expirationSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            storage.merge(key, delta, Integer::sum);
            return storage.getOrDefault(key, 0);
        });
    }
}
