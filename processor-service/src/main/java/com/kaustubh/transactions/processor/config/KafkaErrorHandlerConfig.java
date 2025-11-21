package com.kaustubh.transactions.processor.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.kaustubh.transactions.processor.webhook.ProcessorWebhookRecoverer;
import com.kaustubh.transactions.processor.service.WebhookDispatchPublisher;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public CommonErrorHandler processorErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            WebhookDispatchPublisher webhookDispatchPublisher) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> consumerRecord, Exception ex) ->
                        new TopicPartition(consumerRecord.topic() + "_dlt", consumerRecord.partition())
        );

        ProcessorWebhookRecoverer webhookRecoverer = new ProcessorWebhookRecoverer(recoverer, webhookDispatchPublisher);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                webhookRecoverer,
                new FixedBackOff(1000L, 2L)
        );

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
