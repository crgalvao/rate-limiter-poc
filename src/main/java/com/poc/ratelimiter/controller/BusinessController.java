package com.poc.ratelimiter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class BusinessController {

    @GetMapping
    public String getProducts(@RequestHeader("X-Client-ID") String clientId) {
        return "Returning list of products for client: " + clientId;
    }
}