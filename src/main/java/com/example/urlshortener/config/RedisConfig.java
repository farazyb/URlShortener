package com.example.urlshortener.config;

import com.example.urlshortener.service.dtos.UrlView;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@EnableCaching
@Configuration
@Log4j2
public class RedisConfig implements CachingConfigurer {

    public static final class Caches {
        public static final String SHORT_URLS = "u";
        public static final String USERS = "usr";
        public static final String LINK_STATS = "stat";

        private Caches() {
        }
    }


    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheWriter redisCacheWriter =
                RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.scan(1000));
        return RedisCacheManager.builder(redisCacheWriter)
                .cacheDefaults(defaultCacheConfiguration())
                .withInitialCacheConfigurations(namedCacheConfigurations())
                .enableStatistics()
                .build();
    }

    private RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(new JitteredTtl(Duration.ofMinutes(10), 0.20d))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(polymorphicJsonSerializer()))
                .computePrefixWith(cacheName -> cacheName + ":");
    }

    private RedisSerializer<Object> polymorphicJsonSerializer() {

        // Never allow unrestricted default typing. A PolymorphicTypeValidator scoped
        // to your own packages is what stops a poisoned cache entry from becoming
        // remote code execution.
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.urlshortener.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubTypeIsArray()
                .build();

        return GenericJacksonJsonRedisSerializer.create(config -> config
                .enableSpringCacheNullValueSupport()   // required to round-trip NullValue
                .customize(mapper -> mapper.activateDefaultTypingAsProperty(
                        typeValidator, DefaultTyping.NON_FINAL, "@class")));
    }

    /**
     * Register a cache here when you want a different TTL, a different serializer,
     * or a different expiry policy. Everything else falls back to the default above.
     */
    private Map<String, RedisCacheConfiguration> namedCacheConfigurations() {

        Map<String, RedisCacheConfiguration> caches = new HashMap<>();

        // Hot path. Typed serializer => no "@class" property in the payload
        // (~40 bytes saved per entry, and no polymorphic deserialization at all).
        caches.put(Caches.SHORT_URLS,
                typedCache(UrlView.class)
                        .entryTtl(new ShortUrlTtl())
                        .enableTimeToIdle()          // GETEX: popular links stay hot, cold ones fall out
                        .disableCachingNullValues()  // we use UrlView.NOT_FOUND as an explicit sentinel
        );

        caches.put(Caches.LINK_STATS,
                typedCache(UrlView.class)  // <- swap for your stats DTO
                        .entryTtl(new JitteredTtl(Duration.ofMinutes(1), 0.30d))
        );

        return caches;
    }

    /**
     * A cache that holds exactly one value type. Prefer this for high-volume caches:
     * smaller payloads, faster, and no polymorphic type handling.
     * <p>
     * Caveat: a typed serializer cannot deserialize Spring's NullValue marker, so
     * either call .disableCachingNullValues() or return a sentinel instance instead
     * of null (see UrlView.NOT_FOUND).
     */
    private <T> RedisCacheConfiguration typedCache(Class<T> type) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(type)))
                .computePrefixWith(cacheName -> cacheName + ":");
    }

    /**
     * Fixed TTL with random jitter.
     * <p>
     * Without jitter, a burst of traffic writes thousands of entries in the same
     * second; six hours later they all expire in the same second and every one of
     * those requests hits Postgres at once. Jitter smears the expiry window out.
     */
    static final class JitteredTtl implements RedisCacheWriter.TtlFunction {

        private final long baseMillis;
        private final long spreadMillis;

        JitteredTtl(Duration base, double ratio) {
            this.baseMillis = base.toMillis();
            this.spreadMillis = (long) (this.baseMillis * ratio);
        }

        @Override
        public Duration getTimeToLive(Object key, Object value) {
            if (spreadMillis <= 0) {
                return Duration.ofMillis(baseMillis);
            }
            long offset = ThreadLocalRandom.current().nextLong(-spreadMillis, spreadMillis + 1);
            return Duration.ofMillis(baseMillis + offset);
        }
    }

    static final class ShortUrlTtl implements RedisCacheWriter.TtlFunction {

        private static final RedisCacheWriter.TtlFunction HIT = new JitteredTtl(Duration.ofHours(6), 0.15d);
        private static final Duration MISS = Duration.ofSeconds(30);

        @Override
        public Duration getTimeToLive(Object key, Object value) {
            if (value instanceof UrlView view && !view.isFound()) {
                return MISS;
            }
            return HIT.getTimeToLive(key, value);
        }
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis cache GET failed [cache={}, key={}] - falling through to origin",
                        cache.getName(), key, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT failed [cache={}, key={}]", cache.getName(), key, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                // The one case worth thinking twice about: a failed evict leaves a
                // stale entry until its TTL runs out. Acceptable only because every
                // cache above has a bounded TTL. Never run a cache with TTL = none.
                log.error("Redis cache EVICT failed [cache={}, key={}] - entry stays stale until TTL",
                        cache.getName(), key, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.error("Redis cache CLEAR failed [cache={}]", cache.getName(), e);
            }
        };
    }

}
