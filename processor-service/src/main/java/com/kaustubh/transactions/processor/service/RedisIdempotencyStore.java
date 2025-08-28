package com.kaustubh.transactions.processor.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.kaustubh.transactions.processor.config.IdempotencyProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore {
    
    private final StringRedisTemplate stringRedisTemplate;
    private final IdempotencyProperties idempotencyProperties;

    public boolean acquire(String idempotencyKey, String transactionId) {
        String redisKey = buildKey(idempotencyKey);

        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
            redisKey,
            transactionId,
            Duration.ofHours(idempotencyProperties.ttlHours())
        );

        return Boolean.TRUE.equals(success);
    }

    public String getExistingTransactionId(String idempotencyKey) {
        return stringRedisTemplate.opsForValue().get(buildKey(idempotencyKey));
    }

    public String buildKey(String idempotencyKey) {
        return idempotencyProperties.keyPrefix() + idempotencyKey;
    }
}
