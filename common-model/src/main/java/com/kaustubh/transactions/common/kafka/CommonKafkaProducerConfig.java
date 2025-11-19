package com.kaustubh.transactions.common.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;

public abstract class CommonKafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> genericProducerFactory(KafkaProperties kafkaProperties) {
        return jsonProducerFactory(kafkaProperties, true);
    }

    @Bean
    public KafkaTemplate<String, Object> genericKafkaTemplate(
            ProducerFactory<String, Object> producerFactory
    ) {
        return kafkaTemplate(producerFactory);
    }

    @Bean
    public ProducerFactory<String, WebhookDispatchEvent> webhookDispatchProducerFactory(
            KafkaProperties kafkaProperties
    ) {
        return jsonProducerFactory(kafkaProperties, false);
    }

    @Bean
    public KafkaTemplate<String, WebhookDispatchEvent> webhookDispatchKafkaTemplate(
            ProducerFactory<String, WebhookDispatchEvent> producerFactory
    ) {
        return kafkaTemplate(producerFactory);
    }

    protected <T> ProducerFactory<String, T> jsonProducerFactory(
            KafkaProperties kafkaProperties,
            boolean addTypeInfoHeaders
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, addTypeInfoHeaders);

        return new DefaultKafkaProducerFactory<>(props);
    }

    protected <T> KafkaTemplate<String, T> kafkaTemplate(ProducerFactory<String, T> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
