package com.kaustubh.transactions.ledger.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.common.webhook.TransactionStatusUpdate;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceTest {

    @Mock
    private TransactionWebhookNotifier webhookNotifier;

    @Test
    void deliverBatch_groupsEventsByCallbackUrl() {
        WebhookDeliveryService service = new WebhookDeliveryService(webhookNotifier);
        List<WebhookDispatchEvent> events = List.of(
                dispatchEvent("https://merchant-a.example/webhook", "tx-1", "ACCEPTED", "corr-1"),
                dispatchEvent("https://merchant-a.example/webhook", "tx-2", "PERSISTED", "corr-2"),
                dispatchEvent("https://merchant-b.example/webhook", "tx-3", "FAILED", "corr-3")
        );

        service.deliverBatch(events);

        verify(webhookNotifier).sendStatusUpdates(
                "https://merchant-a.example/webhook",
                List.of(
                        new TransactionStatusUpdate("tx-1", "ACCEPTED", "corr-1", Instant.parse("2026-03-22T10:10:00Z")),
                        new TransactionStatusUpdate("tx-2", "PERSISTED", "corr-2", Instant.parse("2026-03-22T10:10:00Z"))
                )
        );
        verify(webhookNotifier).sendStatusUpdates(
                "https://merchant-b.example/webhook",
                List.of(new TransactionStatusUpdate("tx-3", "FAILED", "corr-3", Instant.parse("2026-03-22T10:10:00Z")))
        );
    }

    @Test
    void deliverBatch_skipsMissingCallbackUrls() {
        WebhookDeliveryService service = new WebhookDeliveryService(webhookNotifier);

        service.deliverBatch(List.of(
                dispatchEvent(null, "tx-1", "ACCEPTED", "corr-1"),
                dispatchEvent("   ", "tx-2", "FAILED", "corr-2")
        ));

        verifyNoInteractions(webhookNotifier);
    }

    private WebhookDispatchEvent dispatchEvent(
            String callbackUrl,
            String transactionId,
            String status,
            String correlationId
    ) {
        return new WebhookDispatchEvent(
                UUID.randomUUID(),
                callbackUrl,
                transactionId,
                status,
                correlationId,
                Instant.parse("2026-03-22T10:10:00Z")
        );
    }
}
