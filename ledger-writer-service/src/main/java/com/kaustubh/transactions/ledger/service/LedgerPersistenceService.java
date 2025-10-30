package com.kaustubh.transactions.ledger.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.repository.LedgerRepository;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerPersistenceService {

    private final LedgerRepository ledgerRepository;
    private final TransactionWebhookNotifier webhookNotifier;

    public void persistBatch(List<TransactionLogEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        int[][] results = ledgerRepository.batchInsert(events);

        int insertedCount = 0;
        int skippedCount = 0;

        for (int[] batch : results) {
            for (int result : batch) {
                if (result > 0) {
                    insertedCount += result;
                } else {
                    skippedCount++;
                }
            }
        }

        log.info(
                "Ledger batch persisted totalEvents={} inserted={} skippedOrConflicted={}",
                events.size(),
                insertedCount,
                skippedCount
        );

        for (TransactionLogEvent event : events) {
            webhookNotifier.sendStatusUpdate(
                    event.callbackUrl(),
                    TransactionStatus.PERSISTED,
                    event.transactionId(),
                    event.correlationId()
            );
        }
    }
}
