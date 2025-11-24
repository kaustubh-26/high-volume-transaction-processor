package com.kaustubh.transactions.ledger.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

import com.kaustubh.transactions.ledger.webhook.LedgerWebhookRecoverer;
import com.kaustubh.transactions.ledger.service.WebhookDispatchPublisher;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public CommonErrorHandler ledgerErrorHandler(
            @Qualifier("genericKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            WebhookDispatchPublisher webhookDispatchPublisher) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> consumerRecord, Exception ex) ->
                        new TopicPartition(consumerRecord.topic() + "_dlt", consumerRecord.partition())
        );

        LedgerWebhookRecoverer webhookRecoverer = new LedgerWebhookRecoverer(recoverer, webhookDispatchPublisher);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                webhookRecoverer,
                new FixedBackOff(1000L, 2L)
        );

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }

    @Bean
    public CommonErrorHandler webhookErrorHandler(
            @Qualifier("genericKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> consumerRecord, Exception ex) ->
                        new TopicPartition(consumerRecord.topic() + "_dlt", consumerRecord.partition())
        );

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000L);
        backOff.setMaxElapsedTime(15000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
