package com.kaustubh.transactions.common.webhook;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.kaustubh.transactions.common.enums.TransactionStatus;

public class TransactionWebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(TransactionWebhookNotifier.class);

    private final RestTemplate restTemplate;

    public TransactionWebhookNotifier(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendStatusUpdate(
            String callbackUrl,
            TransactionStatus status,
            String transactionId,
            String correlationId
    ) {
        sendStatusUpdate(callbackUrl, status, transactionId, correlationId, Instant.now());
    }

    public void sendStatusUpdate(
            String callbackUrl,
            TransactionStatus status,
            String transactionId,
            String correlationId,
            Instant occurredAt
    ) {
        if (isBlank(callbackUrl)) {
            return;
        }

        TransactionStatusUpdate payload = new TransactionStatusUpdate(
                transactionId,
                status.name(),
                correlationId,
                occurredAt
        );

        try {
            restTemplate.postForEntity(callbackUrl, payload, Void.class);
            log.info("Webhook delivered status={} transactionId={} callbackUrl={}", status, transactionId, callbackUrl);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Webhook delivery failed status=%s transactionId=%s callbackUrl=%s"
                            .formatted(status, transactionId, callbackUrl),
                    ex
            );
        }
    }

    public void sendStatusUpdates(String callbackUrl, List<TransactionStatusUpdate> payload) {
        if (isBlank(callbackUrl) || payload == null || payload.isEmpty()) {
            return;
        }

        try {
            restTemplate.postForEntity(callbackUrl, payload, Void.class);
            log.info(
                    "Webhook batch delivered callbackUrl={} batchSize={}",
                    callbackUrl,
                    payload.size()
            );
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Webhook batch delivery failed callbackUrl=%s batchSize=%d"
                            .formatted(callbackUrl, payload.size()),
                    ex
            );
        }
    }

    private boolean isBlank(String callbackUrl) {
        return callbackUrl == null || callbackUrl.isBlank();
    }
}
