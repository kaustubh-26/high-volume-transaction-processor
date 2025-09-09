package com.kaustubh.transactions.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ledger")
public record LedgerBatchProperties(
        int batchSize) {
}