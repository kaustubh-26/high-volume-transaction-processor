package com.kaustubh.transactions.api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaustubh.transactions.api.security.ReadApiKeyRateLimitFilter;
import com.kaustubh.transactions.api.security.ReadAuthConfigService;
import com.kaustubh.transactions.api.service.TransactionStatusService;
import com.kaustubh.transactions.common.api.TransactionStatusResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transaction API", description = "Transaction submission and status endpoints")
@RequiredArgsConstructor
public class TransactionStatusController {

    private final TransactionStatusService transactionStatusService;
    private final ReadAuthConfigService readAuthConfigService;

    @GetMapping(value = "/{transactionId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get transaction status",
            description = "Returns the latest known status for a transaction when the caller provides a valid read API key and the transaction belongs to an allowed merchant.",
            parameters = {
                    @Parameter(
                            name = "transactionId",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Transaction identifier returned by the create transaction endpoint.",
                            example = "7d9d7f5d-19c4-4d7d-8f2a-6d92c7d90f61"
                    ),
                    @Parameter(
                            name = "X-API-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Read API key used for transaction status access and rate limiting.",
                            example = "read-key-demo"
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction status found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransactionStatusResponse.class),
                            examples = @ExampleObject(
                                    name = "statusFound",
                                    value = """
                                            {
                                              "transactionId": "7d9d7f5d-19c4-4d7d-8f2a-6d92c7d90f61",
                                              "status": "PERSISTED",
                                              "processedAt": "2026-03-22T10:15:30Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid read API key",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransactionController.ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "missingApiKey",
                                            value = """
                                                    {
                                                      "error": "Missing API key"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "invalidApiKey",
                                            value = """
                                                    {
                                                      "error": "Invalid API key"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Transaction exists but is not owned by a merchant allowed for the provided API key", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content),
            @ApiResponse(
                    responseCode = "429",
                    description = "Read API key has exceeded the configured rate limit window",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransactionController.ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "rateLimitExceeded",
                                    value = """
                                            {
                                              "error": "Rate limit exceeded"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<TransactionStatusResponse> getStatus(
            @Parameter(hidden = true)
            @RequestHeader(value = ReadApiKeyRateLimitFilter.HEADER_API_KEY, required = false) String apiKey,
            @Parameter(hidden = true)
            @PathVariable String transactionId
    ) {
        ReadAuthConfigService.ReadAuthConfig config = readAuthConfigService.currentConfig();
        if (apiKey == null || apiKey.isBlank() || !config.isAllowedKey(apiKey)) {
            return ResponseEntity.status(401).<TransactionStatusResponse>build();
        }

        return transactionStatusService.findStatus(transactionId)
                .map(statusRecord -> {
                    if (!config.isMerchantAllowed(apiKey, statusRecord.merchantId())) {
                        return ResponseEntity.status(403).<TransactionStatusResponse>build();
                    }
                    return transactionStatusService.buildResponse(statusRecord)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().<TransactionStatusResponse>build());
                })
                .orElseGet(() -> ResponseEntity.notFound().<TransactionStatusResponse>build());
    }
}
