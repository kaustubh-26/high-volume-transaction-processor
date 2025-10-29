package com.kaustubh.transactions.api.service;

import java.time.Instant;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.api.CreateTransactionRequest;
import com.kaustubh.transactions.common.api.CreateTransactionResponse;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.common.util.IdGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionIngressService {

    private final TransactionEventPublisher transactionEventPublisher;

    public CreateTransactionResponse accept(CreateTransactionRequest request) {
        String transactionId = IdGenerator.newTransactionId();
        String correlationId = MDC.get("correlationId");

        TransactionRequestEvent event = new TransactionRequestEvent(
                IdGenerator.newEventId(),
                transactionId,
                request.idempotencyKey(),
                request.accountId(),
                request.amount(),
                request.currency(),
                request.type(),
                request.callbackUrl(),
                correlationId,
                Instant.now()
        );

        transactionEventPublisher.publish(event);

        return new CreateTransactionResponse(
            transactionId,
            TransactionStatus.RECEIVED.name()
        );
    }
}
