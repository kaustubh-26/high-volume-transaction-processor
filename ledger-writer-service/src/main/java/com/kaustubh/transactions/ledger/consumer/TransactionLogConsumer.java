package com.kaustubh.transactions.ledger.consumer;

import java.util.List;

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

    @KafkaListener(topics = "${app.kafka.topic.transaction-log}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<TransactionLogEvent> events, Acknowledgment acknowledgment) {
        if (events == null || events.isEmpty()) {
            return;
        }
        try {
            ledgerPersistenceService.persistBatch(events);
            acknowledgment.acknowledge();

            log.info(
                    "Acknowledged ledger batch size={} firstTransactionId={} lastTransactionId={}",
                    events.size(),
                    events.getFirst().transactionId(),
                    events.getLast().transactionId());
        } catch (Exception ex) {
            log.error(
                    "Failed to persist ledger batch size={}",
                    events.size(),
                    ex);
            throw ex;
        }
    }
}
