package com.kaustubh.transactions.processor.webhook;

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
import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.processor.service.WebhookDispatchPublisher;

@ExtendWith(MockitoExtension.class)
class ProcessorWebhookRecovererTest {

    @Mock
    private DeadLetterPublishingRecoverer delegate;

    @Mock
    private WebhookDispatchPublisher webhookDispatchPublisher;

    @Test
    void accept_publishesRejectedWebhookDispatchForIllegalArgumentException() {
        ProcessorWebhookRecoverer recoverer = new ProcessorWebhookRecoverer(delegate, webhookDispatchPublisher);
        TransactionRequestEvent event = transactionRequestEvent();
        ConsumerRecord<String, Object> consumerRecord = new ConsumerRecord<>("transaction_request", 0, 0L, "key", event);
        IllegalArgumentException exception = new IllegalArgumentException("bad input");

        recoverer.accept(consumerRecord, exception);

        ArgumentCaptor<WebhookDispatchEvent> eventCaptor = ArgumentCaptor.forClass(WebhookDispatchEvent.class);
        verify(webhookDispatchPublisher).publish(eventCaptor.capture());
        assertDispatch(eventCaptor.getValue(), event, TransactionStatus.REJECTED);
        verify(delegate).accept(consumerRecord, exception);
    }

    @Test
    void accept_publishesFailedWebhookDispatchForNonValidationFailure() {
        ProcessorWebhookRecoverer recoverer = new ProcessorWebhookRecoverer(delegate, webhookDispatchPublisher);
        TransactionRequestEvent event = transactionRequestEvent();
        ConsumerRecord<String, Object> consumerRecord = new ConsumerRecord<>("transaction_request", 0, 0L, "key", event);
        RuntimeException exception = new RuntimeException("boom");

        recoverer.accept(consumerRecord, exception);

        ArgumentCaptor<WebhookDispatchEvent> eventCaptor = ArgumentCaptor.forClass(WebhookDispatchEvent.class);
        verify(webhookDispatchPublisher).publish(eventCaptor.capture());
        assertDispatch(eventCaptor.getValue(), event, TransactionStatus.FAILED);
        verify(delegate).accept(consumerRecord, exception);
    }

    @Test
    void accept_ignoresMissingRecord() {
        ProcessorWebhookRecoverer recoverer = new ProcessorWebhookRecoverer(delegate, webhookDispatchPublisher);
        RuntimeException exception = new RuntimeException("boom");

        recoverer.accept(null, exception);

        verifyNoInteractions(webhookDispatchPublisher, delegate);
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

    private void assertDispatch(
            WebhookDispatchEvent dispatchEvent,
            TransactionRequestEvent requestEvent,
            TransactionStatus status
    ) {
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.callbackUrl()).isEqualTo(requestEvent.callbackUrl());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.transactionId()).isEqualTo(requestEvent.transactionId());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.status()).isEqualTo(status.name());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.correlationId()).isEqualTo(requestEvent.correlationId());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.occurredAt()).isNotNull();
    }
}
