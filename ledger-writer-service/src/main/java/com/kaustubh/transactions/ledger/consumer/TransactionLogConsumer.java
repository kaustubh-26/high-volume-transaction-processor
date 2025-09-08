package com.kaustubh.transactions.ledger.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.service.LedgerPersistenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionLogConsumer {

    private final LedgerPersistenceService ledgerPersistenceService;

    @KafkaListener(
            topics = "${app.kafka.topic.transaction-log}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(TransactionLogEvent event, Acknowledgment acknowledgment) {
        try {
            ledgerPersistenceService.persist(event);
            acknowledgment.acknowledge();

            log.info(
                    "Acknowledged ledger event transactionId={} eventId={}",
                    event.transactionId(),
                    event.eventId()
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to persist ledger event transactionId={} eventId={}",
                    event.transactionId(),
                    event.eventId(),
                    ex
            );
            throw ex;
        }
    }
}
