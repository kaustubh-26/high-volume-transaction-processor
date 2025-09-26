package com.kaustubh.transactions.processor.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS = "redis";
    private static final String LATENCY_MS = "latencyMs";

    @Override
    public Health health() {
        var connectionFactory = redisTemplate.getConnectionFactory();

        if (connectionFactory == null) {
            return Health.down()
                    .withDetail(REDIS, "connectionFactory is null")
                    .build();
        }

        long start = System.nanoTime();
        try (var connection = connectionFactory.getConnection()) {

            String pong = connection.ping();
            long latencyMs = (System.nanoTime() - start) / 1_000_000;

            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up()
                        .withDetail(REDIS, "reachable")
                        .withDetail(LATENCY_MS, latencyMs)
                        .build();
            }

            return Health.down()
                    .withDetail(REDIS, "unexpected ping response")
                    .withDetail(LATENCY_MS, latencyMs)
                    .build();

        } catch (Exception ex) {

            long latencyMs = (System.nanoTime() - start) / 1_000_000;

            return Health.down(ex)
                    .withDetail(REDIS, "unreachable")
                    .withDetail(LATENCY_MS, latencyMs)
                    .build();
        }
    }
}