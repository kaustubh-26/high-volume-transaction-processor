package com.kaustubh.transactions.common.api;

import java.math.BigDecimal;

import com.kaustubh.transactions.common.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request payload for creating a transaction")
public record CreateTransactionRequest(

    @Schema(
        description = "Unique client-generated key used to prevent duplicate submissions",
        example = "idem-20260320-0001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "idempotencyKey is required")
    String idempotencyKey,

    @Schema(
        description = "Unique account identifier",
        example = "acct-1001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "accountId is required")
    String accountId,

    @Schema(
        description = "Transaction amount",
        example = "1500.75",
        minimum = "0.01",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    BigDecimal amount,

    @Schema(
        description = "ISO currency code",
        example = "INR",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "currency is required")
    String currency,

    @Schema(
        description = "Transaction type",
        example = "CREDIT",
        allowableValues = {"DEBIT", "CREDIT", "REFUND", "REVERSAL"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "type is required")
    TransactionType type,

    @JsonAlias("callback_url")
    @Pattern(regexp = "https?://.+", message = "callbackUrl must be a valid http(s) URL")
    String callbackUrl
) {

}
