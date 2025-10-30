package com.kaustubh.transactions.common.webhook;

import java.time.Instant;

public record TransactionStatusUpdate(
    String transactionId,
    String status,
    String correlationId,
    Instant occurredAt
) {
}
