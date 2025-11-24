package com.kaustubh.transactions.ledger.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.common.event.WebhookDispatchEvent;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final LedgerKafkaListenerProperties listenerProperties;

    @Bean
    public ConsumerFactory<String, TransactionLogEvent> transactionLogConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kaustubh.transactions.common.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionLogEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionLogEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, TransactionLogEvent> transactionLogConsumerFactory,
            @org.springframework.beans.factory.annotation.Qualifier("ledgerErrorHandler")
            CommonErrorHandler ledgerErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, TransactionLogEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(transactionLogConsumerFactory);
        factory.setBatchListener(true);
        factory.setConcurrency(listenerProperties.concurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(ledgerErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, WebhookDispatchEvent> webhookDispatchConsumerFactory(
            KafkaProperties kafkaProperties,
            WebhookDeliveryProperties webhookDeliveryProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kaustubh.transactions.common.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, WebhookDispatchEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, webhookDeliveryProperties.maxPollRecords());

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WebhookDispatchEvent> webhookKafkaListenerContainerFactory(
            ConsumerFactory<String, WebhookDispatchEvent> webhookDispatchConsumerFactory,
            @org.springframework.beans.factory.annotation.Qualifier("webhookErrorHandler") CommonErrorHandler webhookErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WebhookDispatchEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(webhookDispatchConsumerFactory);
        factory.setBatchListener(true);
        factory.setConcurrency(listenerProperties.concurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(webhookErrorHandler);

        return factory;
    }
}
