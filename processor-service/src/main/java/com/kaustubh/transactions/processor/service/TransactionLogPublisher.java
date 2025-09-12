package com.kaustubh.transactions.processor.service;

import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.processor.config.KafkaTopicProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionLogPublisher {
    private final KafkaTemplate<String, TransactionLogEvent> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;

    public void publish(TransactionLogEvent event) {
        try {
            // accountId is the partition key to preserve per-account event order
            var result = kafkaTemplate.send(
                    kafkaTopicProperties.transactionLog(),
                    event.accountId(),
                    event
            ).get();

            log.info(
                    "Published transaction log event transactionId={} topic={} partition={} offset={}",
                    event.transactionId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing transaction log event", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Failed to publish transaction log event", ex);
        }
    }
}
