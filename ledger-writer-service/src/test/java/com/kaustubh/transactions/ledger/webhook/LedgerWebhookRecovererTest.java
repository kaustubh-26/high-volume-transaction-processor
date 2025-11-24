package com.kaustubh.transactions.ledger.webhook;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.ledger.service.WebhookDispatchPublisher;

@ExtendWith(MockitoExtension.class)
class LedgerWebhookRecovererTest {

    @Mock
    private DeadLetterPublishingRecoverer delegate;

    @Mock
    private WebhookDispatchPublisher webhookDispatchPublisher;

    @Test
    void accept_publishesFailedWebhookDispatchForTransactionLogEvent() {
        LedgerWebhookRecoverer recoverer = new LedgerWebhookRecoverer(delegate, webhookDispatchPublisher);
        TransactionLogEvent event = transactionLogEvent();
        ConsumerRecord<String, Object> consumerRecord = new ConsumerRecord<>("transaction_log", 0, 0L, "key", event);
        RuntimeException exception = new RuntimeException("boom");

        recoverer.accept(consumerRecord, exception);

        ArgumentCaptor<WebhookDispatchEvent> eventCaptor = ArgumentCaptor.forClass(WebhookDispatchEvent.class);
        verify(webhookDispatchPublisher).publish(eventCaptor.capture());
        assertDispatch(eventCaptor.getValue(), event, TransactionStatus.FAILED);
        verify(delegate).accept(consumerRecord, exception);
    }

    @Test
    void accept_delegatesWithoutWebhookForUnknownPayload() {
        LedgerWebhookRecoverer recoverer = new LedgerWebhookRecoverer(delegate, webhookDispatchPublisher);
        ConsumerRecord<String, Object> consumerRecord = new ConsumerRecord<>("transaction_log", 0, 0L, "key", "not-an-event");
        RuntimeException exception = new RuntimeException("boom");

        recoverer.accept(consumerRecord, exception);

        verifyNoInteractions(webhookDispatchPublisher);
        verify(delegate).accept(consumerRecord, exception);
    }

    @Test
    void accept_ignoresMissingRecord() {
        LedgerWebhookRecoverer recoverer = new LedgerWebhookRecoverer(delegate, webhookDispatchPublisher);
        RuntimeException exception = new RuntimeException("boom");

        recoverer.accept(null, exception);

        verifyNoInteractions(webhookDispatchPublisher, delegate);
    }

    private TransactionLogEvent transactionLogEvent() {
        return new TransactionLogEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "acct-1",
                new BigDecimal("100.00"),
                "INR",
                TransactionType.CREDIT,
                TransactionStatus.PERSISTED,
                "https://merchant.example/webhook",
                "corr-1",
                Instant.parse("2026-03-22T10:15:30Z")
        );
    }

    private void assertDispatch(
            WebhookDispatchEvent dispatchEvent,
            TransactionLogEvent transactionLogEvent,
            TransactionStatus status
    ) {
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.callbackUrl()).isEqualTo(transactionLogEvent.callbackUrl());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.transactionId()).isEqualTo(transactionLogEvent.transactionId());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.status()).isEqualTo(status.name());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.correlationId()).isEqualTo(transactionLogEvent.correlationId());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.occurredAt()).isNotNull();
    }
}
