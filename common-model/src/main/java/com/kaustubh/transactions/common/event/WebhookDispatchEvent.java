package com.kaustubh.transactions.common.event;

import java.time.Instant;
import java.util.UUID;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.util.IdGenerator;
import com.kaustubh.transactions.common.webhook.TransactionStatusUpdate;

public record WebhookDispatchEvent(
        UUID eventId,
        String callbackUrl,
        String transactionId,
        String status,
        String correlationId,
        Instant occurredAt
) {

    public static WebhookDispatchEvent statusUpdate(
            String callbackUrl,
            String transactionId,
            TransactionStatus status,
            String correlationId,
            Instant occurredAt
    ) {
        return new WebhookDispatchEvent(
                IdGenerator.newEventId(),
                callbackUrl,
                transactionId,
                status.name(),
                correlationId,
                occurredAt
        );
    }

    public TransactionStatusUpdate toStatusUpdate() {
        return new TransactionStatusUpdate(
                transactionId,
                status,
                correlationId,
                occurredAt
        );
    }
}
