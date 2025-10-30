package com.kaustubh.transactions.common.webhook;

import java.time.Instant;

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
        if (callbackUrl == null || callbackUrl.isBlank()) {
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
        } catch (Exception ex) {
            log.warn(
                    "Webhook delivery failed status={} transactionId={} callbackUrl={}",
                    status,
                    transactionId,
                    callbackUrl,
                    ex
            );
        }
    }
}
