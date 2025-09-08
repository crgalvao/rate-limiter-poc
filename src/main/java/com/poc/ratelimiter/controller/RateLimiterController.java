package com.poc.ratelimiter.controller;

import com.poc.ratelimiter.application.RateLimiterService;
import com.poc.ratelimiter.config.ClientRateLimitConfig;
import com.poc.ratelimiter.domain.DistributedHighThroughputRateLimiter;
import com.poc.ratelimiter.infrastructure.InMemoryDistributedKeyValueStore;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/ratelimit")
public class RateLimiterController {

    private final RateLimiterService service;

    public RateLimiterController(ClientRateLimitConfig config) {
        var store = new InMemoryDistributedKeyValueStore();
        var limiter = new DistributedHighThroughputRateLimiter(store);
        this.service = new RateLimiterService(limiter, config);
    }

    @GetMapping
    public String checkLimit(@RequestParam String clientId)
            throws ExecutionException, InterruptedException {

        boolean allowed = service.isRequestAllowed(clientId).get();
        return "Client " + clientId + " allowed? " + allowed;
    }
}
