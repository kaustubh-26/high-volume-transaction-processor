package com.kaustubh.transactions.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Acknowledgment returned after a transaction is accepted for asynchronous processing")
public record CreateTransactionResponse(
    @Schema(
        description = "Server-generated transaction identifier",
        example = "7d9d7f5d-19c4-4d7d-8f2a-6d92c7d90f61"
    )
    String transactionId,

    @Schema(
        description = "Current transaction status at ingress time",
        example = "RECEIVED",
        allowableValues = {"RECEIVED", "ACCEPTED", "REJECTED", "PERSISTED", "FAILED"}
    )
    String status
) {
    
}
