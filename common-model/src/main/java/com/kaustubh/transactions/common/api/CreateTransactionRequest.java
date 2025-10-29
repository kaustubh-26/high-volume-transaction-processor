package com.kaustubh.transactions.common.api;

import java.math.BigDecimal;

import com.kaustubh.transactions.common.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTransactionRequest(
    @NotBlank(message = "idempotencyKey is required")
    String idempotencyKey,

    @NotBlank(message = "accountId is required")
    String accountId,

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    BigDecimal amount,

    @NotBlank(message = "currency is required")
    String currency,

    @NotNull(message = "type is required")
    TransactionType type,

    @JsonAlias("callback_url")
    @Pattern(regexp = "https?://.+", message = "callbackUrl must be a valid http(s) URL")
    String callbackUrl
) {

}
