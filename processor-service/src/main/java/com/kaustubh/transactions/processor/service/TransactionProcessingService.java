package com.kaustubh.transactions.processor.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.common.util.IdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessingService {

    private final RedisIdempotencyStore redisIdempotencyStore;

    public ProcessingResult process(TransactionRequestEvent event) {
        validate(event);

        boolean acquired = redisIdempotencyStore.acquire(
                event.idempotencyKey(),
                event.transactionId());

        if (!acquired) {
            String existingTransactionId = redisIdempotencyStore.getExistingTransactionId(event.idempotencyKey());

            if (event.transactionId().equals(existingTransactionId)) {
                log.info(
                        "Replaying accepted transaction request idempotencyKey={} transactionId={}",
                        event.idempotencyKey(),
                        event.transactionId()
                );
                return ProcessingResult.duplicateReplayResult();
            }

            log.warn(
                    "Duplicate transaction request detected idempotencyKey={} incomingTransactionId={} existingTransactionId={}",
                    event.idempotencyKey(),
                    event.transactionId(),
                    existingTransactionId
            );

            return ProcessingResult.duplicateRejectedResult();
        }

        TransactionLogEvent transactionLogEvent = new TransactionLogEvent(
                IdGenerator.newEventId(),
                event.transactionId(),
                event.idempotencyKey(),
                event.merchantId(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.type(),
                TransactionStatus.ACCEPTED,
                event.callbackUrl(),
                event.correlationId(),
                Instant.now()
        );

        log.info(
                "Processed transaction request transactionId={} accountId={} amount={} currency={} type={}",
                event.transactionId(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.type());

        return ProcessingResult.processed(transactionLogEvent);
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
