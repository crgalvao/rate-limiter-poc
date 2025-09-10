package com.poc.ratelimiter.interceptor;

import com.poc.ratelimiter.application.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimiterInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        final String clientId = request.getHeader("X-Client-ID");

        if (clientId == null || clientId.isBlank()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing X-Client-ID header");
            return false;
        }

        boolean allowed = rateLimiterService.isRequestAllowed(clientId).get();

        if (allowed) {
            return true;
        } else {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "You have exceeded your request limit.");
            return false;
        }
    }
}