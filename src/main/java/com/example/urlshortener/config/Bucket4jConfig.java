package com.example.urlshortener.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class Bucket4jConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Bean
    public RedisClient redisClient() {   //(1)
        return RedisClient.create(RedisURI.builder()
                        .withHost(host)
                        .withPort(port)
                .withSsl(false)
                .build());
    }

    @Bean
    public ProxyManager<String>
    lettuceBasedProxyManager(RedisClient redisClient) { //(2)
        StatefulRedisConnection<String,byte[]> redisConnection = redisClient.
                connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        return LettuceBasedProxyManager.builderFor(redisConnection)
                .build();
    }

    @Bean
    public BucketConfiguration bucketConfiguration() { //(3)
        return BucketConfiguration.builder()
                .addLimit(limit-> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(1)))
                .build();
    }
}
