package com.kaustubh.transactions.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(
    String keyPrefix,
    long ttlHours
) {
}
