package com.example.urlshortener.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Plain spring-web filter (no Spring Security involved).
 * Registered automatically because it's a Filter bean.
 * <p>
 * Keys buckets by client IP here; swap resolveClientKey() for
 * an API key / user id if you have auth in place.
 */
@Component
@Log4j2
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentMap<String, RateLimiter> buckets = new ConcurrentHashMap<>();
    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration configuration;

    // tune these to taste: 20 requests per 10-second window, per client
//    private static final long CAPACITY = 5;
//    private static final long REFILL_TOKENS = 5;
//    private static final long REFILL_PERIOD_MILLIS = 60_000;

    public RateLimitFilter(ProxyManager<String> proxyManager, BucketConfiguration configuration) {
        this.proxyManager = proxyManager;
        this.configuration = configuration;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientKey = resolveClientKey(request);
        Bucket bucket=proxyManager.getProxy(clientKey,()->configuration);

//        RateLimiter limiter = buckets.computeIfAbsent(clientKey,
//                key -> new TokenBucketRateLimiter(CAPACITY, REFILL_TOKENS, REFILL_PERIOD_MILLIS));

        if (bucket.tryConsume(1L)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(bucket.estimateAbilityToConsume(1L).getNanosToWaitForRefill() / 1000));
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
}

