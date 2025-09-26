package com.kaustubh.transactions.processor.health;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisHealthIndicatorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    private RedisHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new RedisHealthIndicator(redisTemplate);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
    }

    @Test
    void health_reportsUpOnPong() {
        when(connection.ping()).thenReturn("PONG");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("redis", "reachable");
    }

    @Test
    void health_reportsDownOnUnexpectedPing() {
        when(connection.ping()).thenReturn("NOPE");

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("redis", "unexpected ping response");
    }

    @Test
    void health_reportsDownOnException() {
        when(connection.ping()).thenThrow(new IllegalStateException("boom"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("redis", "unreachable");
    }
}
