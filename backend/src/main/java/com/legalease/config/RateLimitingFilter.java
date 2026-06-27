package com.legalease.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Simple Token Bucket rate limiter per IP address
    private static class TokenBucket {
        private final long capacity;
        private final long refillPeriodMs;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(long capacity, long refillPeriodMs) {
            this.capacity = capacity;
            this.refillPeriodMs = refillPeriodMs;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long duration = now - lastRefillTime;
            double tokensToAdd = (double) duration / refillPeriodMs;
            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }

    // Limit to 30 requests per minute per IP to protect resources
    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();
    private static final long LIMIT_CAPACITY = 30;
    private static final long REFILL_PERIOD_MS = TimeUnit.MINUTES.toMillis(1) / LIMIT_CAPACITY; // 1 token every 2 seconds

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Skip rate limiting for OPTIONS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Extract IP Address as the client identifier
        String clientIp = getClientIp(request);
        TokenBucket bucket = ipBuckets.computeIfAbsent(clientIp, k -> new TokenBucket(LIMIT_CAPACITY, REFILL_PERIOD_MS));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for client IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
