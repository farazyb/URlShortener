package com.example.urlshortener.security;

public interface RateLimiter {
     boolean tryAcquire();
}
