package com.kaustubh.transactions.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kaustubh.transactions.api.repository.TransactionStatusRepository.TransactionStatusRecord;
import com.kaustubh.transactions.api.security.ReadAuthConfigService;
import com.kaustubh.transactions.api.security.ReadAuthConfigService.ReadAuthConfig;
import com.kaustubh.transactions.api.service.TransactionStatusService;
import com.kaustubh.transactions.common.api.TransactionStatusResponse;

@ExtendWith(MockitoExtension.class)
class TransactionStatusControllerTest {

    @Mock
    private TransactionStatusService transactionStatusService;

    @Mock
    private ReadAuthConfigService readAuthConfigService;

    @Test
    void getStatus_returnsUnauthorizedWhenApiKeyMissing() {
        TransactionStatusController controller = new TransactionStatusController(
                transactionStatusService,
                readAuthConfigService
        );
        when(readAuthConfigService.currentConfig()).thenReturn(config());

        ResponseEntity<TransactionStatusResponse> response = controller.getStatus(null, "tx-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getStatus_returnsNotFoundWhenTransactionMissing() {
        TransactionStatusController controller = new TransactionStatusController(
                transactionStatusService,
                readAuthConfigService
        );
        when(readAuthConfigService.currentConfig()).thenReturn(config());
        when(transactionStatusService.findStatus("tx-1")).thenReturn(Optional.empty());

        ResponseEntity<TransactionStatusResponse> response = controller.getStatus("key-1", "tx-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getStatus_returnsForbiddenWhenMerchantDoesNotOwnTransaction() {
        TransactionStatusController controller = new TransactionStatusController(
                transactionStatusService,
                readAuthConfigService
        );
        when(readAuthConfigService.currentConfig()).thenReturn(config());
        when(transactionStatusService.findStatus("tx-1")).thenReturn(Optional.of(createStatusRecord("merchant-2")));

        ResponseEntity<TransactionStatusResponse> response = controller.getStatus("key-1", "tx-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getStatus_returnsStatusBodyForAuthorizedMerchant() {
        TransactionStatusController controller = new TransactionStatusController(
                transactionStatusService,
                readAuthConfigService
        );
        TransactionStatusRecord statusRecord = createStatusRecord("merchant-1");
        TransactionStatusResponse body = new TransactionStatusResponse(
                "tx-1",
                "PERSISTED",
                Instant.parse("2026-03-22T10:15:30Z")
        );
        when(readAuthConfigService.currentConfig()).thenReturn(config());
        when(transactionStatusService.findStatus("tx-1")).thenReturn(Optional.of(statusRecord));
        when(transactionStatusService.buildResponse(statusRecord)).thenReturn(Optional.of(body));

        ResponseEntity<TransactionStatusResponse> response = controller.getStatus("key-1", "tx-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(body);
    }

    private ReadAuthConfig config() {
        return new ReadAuthConfig(
                Set.of("key-1"),
                Map.of("key-1", 10),
                Map.of("key-1", Set.of("merchant-1")),
                60,
                10
        );
    }

    private TransactionStatusRecord createStatusRecord(String merchantId) {
        return new TransactionStatusRecord(
                "tx-1",
                merchantId,
                "PERSISTED",
                Instant.parse("2026-03-22T10:15:30Z")
        );
    }
}
