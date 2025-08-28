package com.kaustubh.transactions.processor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.kaustubh.transactions.processor.config.IdempotencyProperties;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new RedisIdempotencyStore(
                stringRedisTemplate,
                new IdempotencyProperties("idem:", 2)
        );
    }

    @Test
    void buildKey_prefixesIdempotencyKey() {
        assertThat(store.buildKey("abc")).isEqualTo("idem:abc");
    }

    @Test
    void acquire_returnsTrueWhenSetIfAbsentSucceeds() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("idem:abc", "tx-1", Duration.ofHours(2)))
                .thenReturn(true);

        boolean acquired = store.acquire("abc", "tx-1");

        assertThat(acquired).isTrue();
        verify(valueOperations).setIfAbsent("idem:abc", "tx-1", Duration.ofHours(2));
    }

    @Test
    void acquire_returnsFalseWhenSetIfAbsentFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("idem:abc", "tx-1", Duration.ofHours(2)))
                .thenReturn(false);

        boolean acquired = store.acquire("abc", "tx-1");

        assertThat(acquired).isFalse();
    }

    @Test
    void getExistingTransactionId_readsFromRedis() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idem:abc")).thenReturn("tx-9");

        String existing = store.getExistingTransactionId("abc");

        assertThat(existing).isEqualTo("tx-9");
        verify(valueOperations).get("idem:abc");
    }
}
