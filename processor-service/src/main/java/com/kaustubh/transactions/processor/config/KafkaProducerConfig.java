package com.kaustubh.transactions.processor.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.kaustubh.transactions.common.event.TransactionLogEvent;

@Configuration
public class KafkaProducerConfig {

    private Map<String, Object> baseProducerProps(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return props;
    }

    @Bean
    public ProducerFactory<String, TransactionLogEvent> transactionLogProducerFactory(
            KafkaProperties kafkaProperties) {

        Map<String, Object> props = baseProducerProps(kafkaProperties);
        
        // Explicitly disable type headers for strongly typed events
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaProducerFactory<>(props);
    }
    
    @Bean
    public ProducerFactory<String, Object> genericProducerFactory(
            KafkaProperties kafkaProperties) {
                
        Map<String, Object> props = baseProducerProps(kafkaProperties);
        
        // Generic producer supports polymorphic events
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> genericKafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public KafkaTemplate<String, TransactionLogEvent> transactionLogKafkaTemplate(
            ProducerFactory<String, TransactionLogEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
