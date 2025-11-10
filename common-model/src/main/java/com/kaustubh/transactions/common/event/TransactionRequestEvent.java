package com.kaustubh.transactions.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.kaustubh.transactions.common.enums.TransactionType;

public record TransactionRequestEvent(
    UUID eventId,
    String transactionId,
    String idempotencyKey,
    String merchantId,
    String accountId,
    BigDecimal amount,
    String currency,
    TransactionType type,
    String callbackUrl,
    String correlationId,
    Instant createdAt
) {
    
}
