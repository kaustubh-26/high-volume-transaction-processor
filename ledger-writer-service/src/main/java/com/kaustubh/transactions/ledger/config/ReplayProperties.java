package com.kaustubh.transactions.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.replay")
public record ReplayProperties(
        boolean enabledOnStartup,
        String consumerGroupId,
        int maxPollRecords,
        long pollTimeoutMs,
        long idleStopAfterPolls
) {
}