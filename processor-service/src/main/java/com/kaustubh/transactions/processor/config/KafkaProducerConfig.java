package com.kaustubh.transactions.processor.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.common.kafka.CommonKafkaProducerConfig;

@Configuration
public class KafkaProducerConfig extends CommonKafkaProducerConfig {

    @Bean
    public ProducerFactory<String, TransactionLogEvent> transactionLogProducerFactory(
            KafkaProperties kafkaProperties
    ) {
        return jsonProducerFactory(kafkaProperties, false);
    }

    @Bean
    public KafkaTemplate<String, TransactionLogEvent> transactionLogKafkaTemplate(
            ProducerFactory<String, TransactionLogEvent> producerFactory
    ) {
        return kafkaTemplate(producerFactory);
    }
}
