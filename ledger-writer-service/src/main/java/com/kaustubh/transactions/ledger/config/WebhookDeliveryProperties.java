package com.kaustubh.transactions.ledger.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhook.delivery")
public record WebhookDeliveryProperties(
        int maxPollRecords,
        Duration connectTimeout,
        Duration readTimeout
) {
}
