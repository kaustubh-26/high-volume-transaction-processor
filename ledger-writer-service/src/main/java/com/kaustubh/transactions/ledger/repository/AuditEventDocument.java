package com.kaustubh.transactions.ledger.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "transaction_audit_events")
public record AuditEventDocument(
        @Id
        String id,
        UUID eventId,
        String transactionId,
        String idempotencyKey,
        String accountId,
        BigDecimal amount,
        String currency,
        String type,
        String status,
        Instant processedAt,
        Instant storedAt,
        String sourceTopic
) {
}