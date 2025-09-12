package com.kaustubh.transactions.api.service;

import java.util.Objects;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.kaustubh.transactions.api.config.KafkaTopicProperties;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventPublisher {
    private final KafkaTemplate<String, TransactionRequestEvent> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;

    public void publish(TransactionRequestEvent event) {
        TransactionRequestEvent safeEvent = Objects.requireNonNull(
            event,
            "transactionRequestEvent must not be null"
        );
        String topic = Objects.requireNonNull(
                kafkaTopicProperties.transactionRequests(),
                "Kafka topic app.kafka.topic.transaction-requests must not be null"
        );

        String accountId = Objects.requireNonNull(
                safeEvent.accountId(),
                "accountId must not be null"
        );
        // accountId is the partition key to preserve per-account event order
        kafkaTemplate.send(
            topic,
            accountId,
            safeEvent
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish transaction request event transactionId={}", event.transactionId(), ex);
                return;
            }

            log.info(
                "Published transaction request event transactionId={} topic={} partition={} offset={}",
                event.transactionId(),
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()
            );
        });
    }
}
