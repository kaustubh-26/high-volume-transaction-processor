package com.kaustubh.transactions.common.api;

import java.time.Instant;

public record TransactionStatusResponse(
    String transactionId,
    String status,
    Instant processedAt
) {
}
