package com.kaustubh.transactions.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.reconciliation")
public record ReconciliationProperties(
        boolean enabled,
        long fixedDelayMs,
        long lookbackMinutes
) {
}