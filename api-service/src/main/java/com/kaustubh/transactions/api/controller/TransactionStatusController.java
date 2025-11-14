package com.kaustubh.transactions.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.kaustubh.transactions.api.security.ReadApiKeyRateLimitFilter;
import com.kaustubh.transactions.api.security.ReadAuthConfigService;
import com.kaustubh.transactions.api.service.TransactionStatusService;
import com.kaustubh.transactions.common.api.TransactionStatusResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionStatusController {

    private final TransactionStatusService transactionStatusService;
    private final ReadAuthConfigService readAuthConfigService;

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<TransactionStatusResponse> getStatus(
            @RequestHeader(value = ReadApiKeyRateLimitFilter.HEADER_API_KEY, required = false) String apiKey,
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
