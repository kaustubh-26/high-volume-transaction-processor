package com.kaustubh.transactions.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.kaustubh.transactions.api.config.ReadAuthProperties;
import com.kaustubh.transactions.api.security.ReadAuthConfigService.ReadAuthConfig;

@ExtendWith(MockitoExtension.class)
class ReadAuthConfigServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    void currentConfig_usesRedisOverridesAndParsesValues() {
        ReadAuthProperties properties = properties();
        ReadAuthConfigService service = new ReadAuthConfigService(properties, redisTemplate);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("read-auth:config")).thenReturn(Map.of(
                "api_keys", "key-1,key-2",
                "rate_limits", "key-1:15,key-2:not-a-number",
                "api_key_merchants", "key-1:merchant-1|merchant-2,key-2:merchant-3",
                "rate_window_seconds", "120",
                "default_limit_per_window", "25"
        ));

        ReadAuthConfig config = service.currentConfig();

        assertThat(config.allowedKeys()).containsExactlyInAnyOrder("key-1", "key-2");
        assertThat(config.perKeyLimits()).containsEntry("key-1", 15);
        assertThat(config.resolveLimit("key-2")).isEqualTo(25);
        assertThat(config.windowSeconds()).isEqualTo(120);
        assertThat(config.isAllowedKey("key-1")).isTrue();
        assertThat(config.isMerchantAllowed("key-1", "merchant-2")).isTrue();
        assertThat(config.isMerchantAllowed("key-1", "merchant-9")).isFalse();
    }

    @Test
    void currentConfig_fallsBackToPropertiesWhenRedisFailsOrValuesInvalid() {
        ReadAuthProperties properties = properties();
        properties.setApiKeys("fallback-key");
        properties.setRateLimits("fallback-key:7");
        properties.setApiKeyMerchants("fallback-key:merchant-9");
        properties.setRateWindowSeconds(0);
        properties.setDefaultLimitPerWindow(0);
        ReadAuthConfigService service = new ReadAuthConfigService(properties, redisTemplate);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("read-auth:config")).thenThrow(new RuntimeException("redis down"));

        ReadAuthConfig config = service.currentConfig();

        assertThat(config.allowedKeys()).containsExactly("fallback-key");
        assertThat(config.resolveLimit("fallback-key")).isEqualTo(7);
        assertThat(config.windowSeconds()).isEqualTo(1);
        assertThat(config.defaultLimit()).isEqualTo(1);
        assertThat(config.isMerchantAllowed("fallback-key", "merchant-9")).isTrue();
    }

    @Test
    void currentConfig_ignoresMalformedEntriesAndBlankOverrides() {
        ReadAuthProperties properties = properties();
        ReadAuthConfigService service = new ReadAuthConfigService(properties, redisTemplate);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("read-auth:config")).thenReturn(Map.of(
                "api_keys", "   ",
                "rate_limits", "broken,key-1:,key-2:4",
                "api_key_merchants", "missing,key-1:merchant-1||merchant-2,key-2:",
                "rate_window_seconds", "not-a-number",
                "default_limit_per_window", " "
        ));

        ReadAuthConfig config = service.currentConfig();

        assertThat(config.allowedKeys()).containsExactly("default-key");
        assertThat(config.perKeyLimits()).containsExactly(Map.entry("key-2", 4));
        assertThat(config.keyMerchants()).containsOnlyKeys("key-1");
        assertThat(config.keyMerchants().get("key-1")).containsExactlyInAnyOrder("merchant-1", "merchant-2");
        assertThat(config.windowSeconds()).isEqualTo(60);
        assertThat(config.defaultLimit()).isEqualTo(10);
    }

    private ReadAuthProperties properties() {
        ReadAuthProperties properties = new ReadAuthProperties();
        properties.setRedisConfigKey("read-auth:config");
        properties.setApiKeys("default-key");
        properties.setRateLimits("default-key:5");
        properties.setApiKeyMerchants("default-key:merchant-default");
        properties.setRateWindowSeconds(60);
        properties.setDefaultLimitPerWindow(10);
        return properties;
    }
}
