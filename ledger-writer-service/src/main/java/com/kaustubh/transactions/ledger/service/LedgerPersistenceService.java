package com.kaustubh.transactions.ledger.service;

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

    public void persist(TransactionLogEvent event) {
        int rowsInserted = ledgerRepository.insert(event);

        if (rowsInserted == 0) {
            log.warn(
                    "Ledger entry already exists transactionId={} idempotencyKey={}",
                    event.transactionId(),
                    event.idempotencyKey()
            );
            return;
        }

        log.info(
                "Ledger entry persisted transactionId={} accountId={} amount={} currency={} status={}",
                event.transactionId(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.status()
        );
    }
}
