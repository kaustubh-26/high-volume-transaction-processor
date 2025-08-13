package com.kaustubh.transactions.common.api;

public record CreateTransactionResponse(
    String transactionId,
    String status
) {
    
}
