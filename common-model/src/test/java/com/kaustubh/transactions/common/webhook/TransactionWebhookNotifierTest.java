package com.kaustubh.transactions.common.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.kaustubh.transactions.common.enums.TransactionStatus;

class TransactionWebhookNotifierTest {

    @Test
    void sendStatusUpdate_postsPayloadWhenCallbackProvided() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        TransactionWebhookNotifier notifier = new TransactionWebhookNotifier(restTemplate);

        String callbackUrl = "http://example.test/webhook";
        Instant occurredAt = Instant.parse("2026-03-22T10:10:00Z");

        TransactionStatusUpdate expectedPayload = new TransactionStatusUpdate(
                "tx-1",
                TransactionStatus.ACCEPTED.name(),
                "corr-1",
                occurredAt
        );

        when(restTemplate.postForEntity(callbackUrl, expectedPayload, Void.class))
                .thenReturn(ResponseEntity.ok().build());

        notifier.sendStatusUpdate(
                callbackUrl,
                TransactionStatus.ACCEPTED,
                "tx-1",
                "corr-1",
                occurredAt
        );

        verify(restTemplate).postForEntity(callbackUrl, expectedPayload, Void.class);
    }

    @Test
    void sendStatusUpdate_skipsWhenCallbackMissing() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        TransactionWebhookNotifier notifier = new TransactionWebhookNotifier(restTemplate);

        notifier.sendStatusUpdate(
                "   ",
                TransactionStatus.ACCEPTED,
                "tx-1",
                "corr-1"
        );

        verifyNoInteractions(restTemplate);
    }

    @Test
    void sendStatusUpdate_rethrowsWithWebhookContextWhenDeliveryFails() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        TransactionWebhookNotifier notifier = new TransactionWebhookNotifier(restTemplate);

        String callbackUrl = "http://example.test/webhook";
        Instant occurredAt = Instant.parse("2026-03-22T10:12:00Z");
        RuntimeException deliveryFailure = new RuntimeException("boom");
        TransactionStatusUpdate expectedPayload = new TransactionStatusUpdate(
                "tx-2",
                TransactionStatus.FAILED.name(),
                "corr-2",
                occurredAt
        );

        when(restTemplate.postForEntity(callbackUrl, expectedPayload, Void.class))
                .thenThrow(deliveryFailure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> notifier.sendStatusUpdate(
                callbackUrl,
                TransactionStatus.FAILED,
                "tx-2",
                "corr-2",
                occurredAt
        ));

        assertThat(thrown).hasMessage(
                "Webhook delivery failed status=FAILED transactionId=tx-2 callbackUrl=http://example.test/webhook"
        );
        assertThat(thrown.getCause()).isSameAs(deliveryFailure);

        verify(restTemplate).postForEntity(callbackUrl, expectedPayload, Void.class);
        verify(restTemplate, never()).getForEntity(callbackUrl, Void.class);
    }

    @Test
    void sendStatusUpdates_postsBatchPayload() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        TransactionWebhookNotifier notifier = new TransactionWebhookNotifier(restTemplate);

        String callbackUrl = "http://example.test/webhook";
        List<TransactionStatusUpdate> payload = List.of(
                new TransactionStatusUpdate(
                        "tx-1",
                        TransactionStatus.ACCEPTED.name(),
                        "corr-1",
                        Instant.parse("2026-03-22T10:10:00Z")
                ),
                new TransactionStatusUpdate(
                        "tx-2",
                        TransactionStatus.PERSISTED.name(),
                        "corr-2",
                        Instant.parse("2026-03-22T10:11:00Z")
                )
        );

        when(restTemplate.postForEntity(callbackUrl, payload, Void.class))
                .thenReturn(ResponseEntity.ok().build());

        notifier.sendStatusUpdates(callbackUrl, payload);

        verify(restTemplate).postForEntity(callbackUrl, payload, Void.class);
    }

    @Test
    void sendStatusUpdates_rethrowsWithBatchContextWhenDeliveryFails() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        TransactionWebhookNotifier notifier = new TransactionWebhookNotifier(restTemplate);

        String callbackUrl = "http://example.test/webhook";
        List<TransactionStatusUpdate> payload = List.of(
                new TransactionStatusUpdate(
                        "tx-1",
                        TransactionStatus.ACCEPTED.name(),
                        "corr-1",
                        Instant.parse("2026-03-22T10:10:00Z")
                ),
                new TransactionStatusUpdate(
                        "tx-2",
                        TransactionStatus.PERSISTED.name(),
                        "corr-2",
                        Instant.parse("2026-03-22T10:11:00Z")
                )
        );
        RuntimeException deliveryFailure = new RuntimeException("batch boom");

        when(restTemplate.postForEntity(callbackUrl, payload, Void.class))
                .thenThrow(deliveryFailure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> notifier.sendStatusUpdates(callbackUrl, payload));

        assertThat(thrown).hasMessage(
                "Webhook batch delivery failed callbackUrl=http://example.test/webhook batchSize=2"
        );
        assertThat(thrown.getCause()).isSameAs(deliveryFailure);

        verify(restTemplate).postForEntity(callbackUrl, payload, Void.class);
    }
}
