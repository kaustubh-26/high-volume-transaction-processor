package com.kaustubh.transactions.audit.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record TransactionAuditEventDocument(
    @Id
    String id,

    UUID eventId,

    @Indexed
    String transactionId,

    @Indexed
    String idempotencyKey,

    @Indexed
    String accountId,

    @Indexed 
    String correlationId,

    BigDecimal amount,
    String currency,
    String type,
    String status,
    Instant processedAt,
    Instant storedAt,
    String sourceTopic
) {
}