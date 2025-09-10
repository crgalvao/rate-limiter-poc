package com.poc.ratelimiter.infrastructure;

import com.poc.ratelimiter.domain.DistributedKeyValueStore;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryDistributedKeyValueStore implements DistributedKeyValueStore {

    private final Map<String, AtomicInteger> storage = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Integer> incrementByAndExpire(String key, int delta, int expirationSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger currentValue = storage.computeIfAbsent(key, k -> new AtomicInteger(0));
            return currentValue.addAndGet(delta);
        });
    }
}