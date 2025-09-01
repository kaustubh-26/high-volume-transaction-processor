package com.kaustubh.transactions.processor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topic")
public record KafkaTopicProperties(
    String transactionRequests,
    String transactionLog
) {
}
