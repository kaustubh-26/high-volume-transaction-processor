package com.kaustubh.transactions.processor.service;

import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.event.TransactionRequestEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransactionProcessingService {
    
    public void process(TransactionRequestEvent event) {
        validate(event);

        log.info(
                "Processed transaction request transactionId={} accountId={} amount={} currency={} type={}",
                event.transactionId(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.type()
        );
    }

    private void validate(TransactionRequestEvent event) {
        if (event.amount() == null || event.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        if (event.accountId() == null || event.accountId().isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }

        if (event.idempotencyKey() == null || event.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        if (event.currency() == null || event.currency().isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        if (event.type() == null) {
            throw new IllegalArgumentException("type must not be null");
        }
    }
}
