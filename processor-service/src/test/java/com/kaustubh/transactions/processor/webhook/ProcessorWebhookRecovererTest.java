package com.kaustubh.transactions.processor.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

@ExtendWith(MockitoExtension.class)
class ProcessorWebhookRecovererTest {

    @Mock
    private DeadLetterPublishingRecoverer delegate;

    @Mock
    private TransactionWebhookNotifier webhookNotifier;

    @Test
    void accept_sendsRejectedWebhookForIllegalArgumentException() {
        ProcessorWebhookRecoverer recoverer = new ProcessorWebhookRecoverer(delegate, webhookNotifier);
        TransactionRequestEvent event = transactionRequestEvent();
        ConsumerRecord<String, Object> consumerRecord = new ConsumerRecord<>("transaction_request", 0, 0L, "key", event);
        IllegalArgumentException exception = new IllegalArgumentException("bad input");

        recoverer.accept(consumerRecord, exception);

        verify(webhookNotifier).sendStatusUpdate(
                eq(event.callbackUrl()),
                eq(TransactionStatus.REJECTED),
                eq(event.transactionId()),
                eq(event.correlationId()),
                any(Instant.class)
        );
        verify(delegate).accept(consumerRecord, exception);
    }

    @Test
    void accept_sendsFailedWebhookForNonValidationFailure() {
        ProcessorWebhookRecoverer recoverer = new ProcessorWebhookRecoverer(delegate, webhookNotifier);
        TransactionRequestEvent event = transactionRequestEvent();
        ConsumerRecord<String, Object> consumerRecord = new ConsumerRecord<>("transaction_request", 0, 0L, "key", event);
        RuntimeException exception = new RuntimeException("boom");

        recoverer.accept(consumerRecord, exception);

        verify(webhookNotifier).sendStatusUpdate(
                eq(event.callbackUrl()),
                eq(TransactionStatus.FAILED),
                eq(event.transactionId()),
                eq(event.correlationId()),
                any(Instant.class)
        );
        verify(delegate).accept(consumerRecord, exception);
    }

    @Test
    void accept_ignoresMissingRecord() {
        ProcessorWebhookRecoverer recoverer = new ProcessorWebhookRecoverer(delegate, webhookNotifier);
        RuntimeException exception = new RuntimeException("boom");

        recoverer.accept(null, exception);

        verifyNoInteractions(webhookNotifier, delegate);
    }

    private TransactionRequestEvent transactionRequestEvent() {
        return new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "acct-1",
                new BigDecimal("100.00"),
                "INR",
                TransactionType.CREDIT,
                "https://merchant.example/webhook",
                "corr-1",
                Instant.parse("2026-03-22T10:15:30Z")
        );
    }
}
