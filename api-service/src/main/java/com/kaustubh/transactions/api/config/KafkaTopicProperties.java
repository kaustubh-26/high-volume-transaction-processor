package com.kaustubh.transactions.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.kafka.topic")
public record KafkaTopicProperties(
    @NotBlank String transactionRequests
) {
}
