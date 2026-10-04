package com.example.urlshortener.security;

import java.util.concurrent.atomic.AtomicLong;

public class TokenBucketRateLimiter implements RateLimiter {
    private int bucketCapacity;
    private int refillRate;
    private final long capacity;
    private final long refillTokens;
    private final long refillPeriodMillis;

    private final AtomicLong availableTokens;
    private final AtomicLong lastRefillTimestamp;

    public TokenBucketRateLimiter(long capacity, long refillTokens, long refillPeriodMillis) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriodMillis = refillPeriodMillis;
        this.availableTokens = new AtomicLong(capacity);
        this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public synchronized boolean tryAcquire() {
        refill();
        if (availableTokens.get() > 0) {
            availableTokens.decrementAndGet();
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTimestamp.get();
        if (elapsed <= 0) {
            return;
        }

        long periodsElapsed = elapsed / refillPeriodMillis;
        if (periodsElapsed > 0) {
            long tokensToAdd = periodsElapsed * refillTokens;
            long newTokenCount = Math.min(capacity, availableTokens.get() + tokensToAdd);
            availableTokens.set(newTokenCount);
            lastRefillTimestamp.addAndGet(periodsElapsed * refillPeriodMillis);
        }
    }
}
