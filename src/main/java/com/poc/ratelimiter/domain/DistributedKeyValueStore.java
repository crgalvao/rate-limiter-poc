package com.poc.ratelimiter.domain;

import java.util.concurrent.CompletableFuture;

public interface DistributedKeyValueStore {
    CompletableFuture<Integer> incrementByAndExpire(String key, int delta, int expirationSeconds);
}
