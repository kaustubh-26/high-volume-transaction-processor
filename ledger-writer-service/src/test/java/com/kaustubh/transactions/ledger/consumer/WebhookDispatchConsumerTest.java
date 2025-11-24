package com.kaustubh.transactions.ledger.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.ledger.service.WebhookDeliveryService;

@ExtendWith(MockitoExtension.class)
class WebhookDispatchConsumerTest {

    @Mock
    private WebhookDeliveryService webhookDeliveryService;

    @Mock
    private Acknowledgment acknowledgment;

    private WebhookDispatchConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new WebhookDispatchConsumer(webhookDeliveryService);
    }

    @Test
    void consume_skipsNullBatch() {
        consumer.consume(null, acknowledgment);

        verify(webhookDeliveryService, never()).deliverBatch(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consume_skipsEmptyBatch() {
        consumer.consume(List.of(), acknowledgment);

        verify(webhookDeliveryService, never()).deliverBatch(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consume_deliversAndAcknowledgesBatch() {
        List<WebhookDispatchEvent> events = List.of(
                dispatchEvent("tx-1", "https://merchant-a.example/webhook"),
                dispatchEvent("tx-2", "https://merchant-b.example/webhook")
        );

        consumer.consume(events, acknowledgment);

        InOrder inOrder = inOrder(webhookDeliveryService, acknowledgment);
        inOrder.verify(webhookDeliveryService).deliverBatch(events);
        inOrder.verify(acknowledgment).acknowledge();
    }

    @Test
    void consume_propagatesExceptionsWithoutAcknowledging() {
        List<WebhookDispatchEvent> events = List.of(
                dispatchEvent("tx-1", "https://merchant.example/webhook")
        );

        doThrow(new IllegalStateException("boom")).when(webhookDeliveryService).deliverBatch(events);

        assertThatThrownBy(() -> consumer.consume(events, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        verify(acknowledgment, never()).acknowledge();
    }

    private WebhookDispatchEvent dispatchEvent(String transactionId, String callbackUrl) {
        return new WebhookDispatchEvent(
                UUID.randomUUID(),
                callbackUrl,
                transactionId,
                "PERSISTED",
                "corr-1",
                Instant.parse("2026-03-22T10:10:00Z")
        );
    }
}
