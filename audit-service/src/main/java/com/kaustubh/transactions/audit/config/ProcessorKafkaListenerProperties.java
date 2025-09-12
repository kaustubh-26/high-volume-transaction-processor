package com.kaustubh.transactions.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.listener")
public record ProcessorKafkaListenerProperties(
        int concurrency
) {
}