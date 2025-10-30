package com.kaustubh.transactions.ledger.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.kaustubh.transactions.ledger.webhook.LedgerWebhookRecoverer;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public CommonErrorHandler ledgerErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            TransactionWebhookNotifier webhookNotifier) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> consumerRecord, Exception ex) ->
                        new TopicPartition(consumerRecord.topic() + "_dlt", consumerRecord.partition())
        );

        LedgerWebhookRecoverer webhookRecoverer = new LedgerWebhookRecoverer(recoverer, webhookNotifier);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                webhookRecoverer,
                new FixedBackOff(1000L, 2L)
        );

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
