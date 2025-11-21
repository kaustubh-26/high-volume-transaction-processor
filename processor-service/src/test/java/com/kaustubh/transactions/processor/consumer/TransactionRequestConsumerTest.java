package com.kaustubh.transactions.processor.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.processor.service.ProcessingResult;
import com.kaustubh.transactions.processor.service.TransactionLogPublisher;
import com.kaustubh.transactions.processor.service.TransactionProcessingService;
import com.kaustubh.transactions.processor.service.WebhookDispatchPublisher;

@ExtendWith(MockitoExtension.class)
class TransactionRequestConsumerTest {

    @Mock
    private TransactionProcessingService transactionProcessingService;

    @Mock
    private TransactionLogPublisher transactionLogPublisher;

    @Mock
    private WebhookDispatchPublisher webhookDispatchPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    private TransactionRequestConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransactionRequestConsumer(
                transactionProcessingService,
                transactionLogPublisher,
                webhookDispatchPublisher
        );
    }

    @Test
    void consume_acknowledgesDuplicateWithoutPublishing() {
        TransactionRequestEvent event = requestEvent();
        when(transactionProcessingService.process(event)).thenReturn(ProcessingResult.duplicateRejectedResult());

        consumer.consume(event, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(transactionLogPublisher, never()).publish(any());
        assertDispatchCaptured(event, TransactionStatus.REJECTED, event.createdAt());
    }

    @Test
    void consume_acknowledgesReplayWithAcceptedWebhookDispatch() {
        TransactionRequestEvent event = requestEvent();
        when(transactionProcessingService.process(event)).thenReturn(ProcessingResult.duplicateReplayResult());

        consumer.consume(event, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(transactionLogPublisher, never()).publish(any());
        assertDispatchCaptured(event, TransactionStatus.ACCEPTED, event.createdAt());
    }

    @Test
    void consume_publishesAndAcknowledgesProcessedEvent() {
        TransactionRequestEvent event = requestEvent();
        TransactionLogEvent logEvent = logEvent();

        when(transactionProcessingService.process(event)).thenReturn(ProcessingResult.processed(logEvent));

        consumer.consume(event, acknowledgment);

        InOrder inOrder = inOrder(transactionLogPublisher, acknowledgment);
        inOrder.verify(transactionLogPublisher).publish(logEvent);
        inOrder.verify(acknowledgment).acknowledge();
        assertDispatchCaptured(logEvent, TransactionStatus.ACCEPTED, logEvent.processedAt());
    }

    @Test
    void consume_propagatesExceptions() {
        TransactionRequestEvent event = requestEvent();
        when(transactionProcessingService.process(event)).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> consumer.consume(event, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        verify(acknowledgment, never()).acknowledge();
    }

    private TransactionRequestEvent requestEvent() {
        return new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "acct-1",
                new BigDecimal("10.00"),
                "USD",
                TransactionType.DEBIT,
                null,
                "corr-1",
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }

    private TransactionLogEvent logEvent() {
        return new TransactionLogEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "acct-1",
                new BigDecimal("10.00"),
                "USD",
                TransactionType.DEBIT,
                TransactionStatus.ACCEPTED,
                null,
                "corr-1",
                Instant.parse("2024-01-01T00:00:10Z")
        );
    }

    private void assertDispatchCaptured(
            TransactionRequestEvent event,
            TransactionStatus expectedStatus,
            Instant expectedOccurredAt
    ) {
        ArgumentCaptor<WebhookDispatchEvent> captor = ArgumentCaptor.forClass(WebhookDispatchEvent.class);
        verify(webhookDispatchPublisher).publish(captor.capture());
        assertDispatch(captor.getValue(), event.callbackUrl(), event.transactionId(), event.correlationId(), expectedStatus, expectedOccurredAt);
    }

    private void assertDispatchCaptured(
            TransactionLogEvent event,
            TransactionStatus expectedStatus,
            Instant expectedOccurredAt
    ) {
        ArgumentCaptor<WebhookDispatchEvent> captor = ArgumentCaptor.forClass(WebhookDispatchEvent.class);
        verify(webhookDispatchPublisher).publish(captor.capture());
        assertDispatch(captor.getValue(), event.callbackUrl(), event.transactionId(), event.correlationId(), expectedStatus, expectedOccurredAt);
    }

    private void assertDispatch(
            WebhookDispatchEvent dispatchEvent,
            String expectedCallbackUrl,
            String expectedTransactionId,
            String expectedCorrelationId,
            TransactionStatus expectedStatus,
            Instant expectedOccurredAt
    ) {
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.callbackUrl()).isEqualTo(expectedCallbackUrl);
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.transactionId()).isEqualTo(expectedTransactionId);
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.correlationId()).isEqualTo(expectedCorrelationId);
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.status()).isEqualTo(expectedStatus.name());
        org.assertj.core.api.Assertions.assertThat(dispatchEvent.occurredAt()).isEqualTo(expectedOccurredAt);
    }
}
