package com.kaustubh.transactions.ledger.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.repository.LedgerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerPersistenceService {

    private final LedgerRepository ledgerRepository;

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
    }
}
