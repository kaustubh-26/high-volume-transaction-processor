package com.kaustubh.transactions.ledger.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

@Configuration
public class WebhookConfig {

    @Bean
    public TransactionWebhookNotifier transactionWebhookNotifier(RestTemplateBuilder restTemplateBuilder) {
        return new TransactionWebhookNotifier(restTemplateBuilder.build());
    }
}
