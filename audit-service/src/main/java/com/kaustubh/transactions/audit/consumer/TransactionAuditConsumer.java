package com.kaustubh.transactions.audit.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.kaustubh.transactions.audit.service.AuditPersistenceService;
import com.kaustubh.transactions.common.event.TransactionLogEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionAuditConsumer {

    private final AuditPersistenceService auditPersistenceService;

    @KafkaListener(
        topics = "${app.kafka.topic.transaction-log}", 
        groupId = "${spring.kafka.consumer.group-id}", 
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            TransactionLogEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            auditPersistenceService.persist(event, topic);
            acknowledgment.acknowledge();

            log.info(
                    "Acknowledged audit event transactionId={} eventId={}",
                    event.transactionId(),
                    event.eventId()
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to persist audit event transactionId={} eventId={}",
                    event.transactionId(),
                    event.eventId(),
                    ex
            );
            throw ex;

        }
    }
}
