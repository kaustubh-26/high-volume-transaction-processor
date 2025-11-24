package com.kaustubh.transactions.ledger.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

@Configuration
public class WebhookConfig {

    @Bean
    @ConditionalOnMissingBean
    public TransactionWebhookNotifier transactionWebhookNotifier(
            RestTemplateBuilder restTemplateBuilder,
            WebhookDeliveryProperties webhookDeliveryProperties
    ) {
        return new TransactionWebhookNotifier(
                restTemplateBuilder
                        .connectTimeout(webhookDeliveryProperties.connectTimeout())
                        .readTimeout(webhookDeliveryProperties.readTimeout())
                        .build()
        );
    }
}
