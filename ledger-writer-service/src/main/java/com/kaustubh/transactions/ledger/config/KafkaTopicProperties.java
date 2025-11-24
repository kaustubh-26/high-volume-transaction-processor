package com.kaustubh.transactions.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topic")
public record KafkaTopicProperties(
        String transactionLog,
        String webhookDispatch
) {
}
