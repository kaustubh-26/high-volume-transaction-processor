package com.kaustubh.transactions.common.webhook;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

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
    void sendStatusUpdate_logsAndSwallowsHttpErrors() {
        RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
        TransactionWebhookNotifier notifier = new TransactionWebhookNotifier(restTemplate);

        String callbackUrl = "http://example.test/webhook";
        TransactionStatusUpdate expectedPayload = new TransactionStatusUpdate(
                "tx-2",
                TransactionStatus.FAILED.name(),
                "corr-2",
                Instant.parse("2026-03-22T10:12:00Z")
        );

        when(restTemplate.postForEntity(callbackUrl, expectedPayload, Void.class))
                .thenThrow(new RuntimeException("boom"));

        notifier.sendStatusUpdate(
                callbackUrl,
                TransactionStatus.FAILED,
                "tx-2",
                "corr-2",
                Instant.parse("2026-03-22T10:12:00Z")
        );

        verify(restTemplate).postForEntity(callbackUrl, expectedPayload, Void.class);
        verify(restTemplate, never()).getForEntity(callbackUrl, Void.class);
    }
}
