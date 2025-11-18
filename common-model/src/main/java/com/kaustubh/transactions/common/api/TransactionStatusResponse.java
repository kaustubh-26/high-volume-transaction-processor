package com.kaustubh.transactions.common.api;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current status view returned by the transaction status endpoint")
public record TransactionStatusResponse(
        @Schema(
                description = "Server-generated transaction identifier",
                example = "7d9d7f5d-19c4-4d7d-8f2a-6d92c7d90f61"
        )
        String transactionId,

        @Schema(
                description = "Latest known transaction status",
                example = "PERSISTED",
                allowableValues = {"RECEIVED", "ACCEPTED", "REJECTED", "PERSISTED", "FAILED"}
        )
        String status,

        @Schema(
                description = "Timestamp when the transaction reached the returned status, if available",
                example = "2026-03-22T10:15:30Z"
        )
        Instant processedAt
) {
}
