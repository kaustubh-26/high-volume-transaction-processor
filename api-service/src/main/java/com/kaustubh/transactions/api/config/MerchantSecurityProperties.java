package com.kaustubh.transactions.api.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record MerchantSecurityProperties(
        Verify verify
) {
    public record Verify(
            long allowedClockSkewSeconds,
            Map<String, MerchantConfig> merchants
    ) {
    }

    public record MerchantConfig(
            String saltKey,
            int saltIndex
    ) {
    }
}