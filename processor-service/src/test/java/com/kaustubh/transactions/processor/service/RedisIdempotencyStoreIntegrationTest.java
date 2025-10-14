package com.kaustubh.transactions.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.kaustubh.transactions.processor.config.IdempotencyProperties;

@Testcontainers(disabledWithoutDocker = true)
class RedisIdempotencyStoreIntegrationTest {

    static {
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        System.setProperty("jna.tmpdir", tmpDir);
        System.setProperty("org.testcontainers.tmpdir", tmpDir);
    }

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Test
    void acquire_and_getExistingTransactionId_workAgainstRedis() {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                REDIS.getHost(),
                REDIS.getMappedPort(6379)
        );
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();

        RedisIdempotencyStore store = new RedisIdempotencyStore(
                template,
                new IdempotencyProperties("test:idem:", 1)
        );

        assertThat(store.acquire("key-1", "tx-1")).isTrue();
        assertThat(store.acquire("key-1", "tx-1")).isFalse();
        assertThat(store.getExistingTransactionId("key-1")).isEqualTo("tx-1");

        connectionFactory.destroy();
    }
}
