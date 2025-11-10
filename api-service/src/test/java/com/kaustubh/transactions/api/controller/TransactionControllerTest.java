package com.kaustubh.transactions.api.controller;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kaustubh.transactions.api.service.TransactionIngressService;
import com.kaustubh.transactions.common.api.CreateTransactionRequest;
import com.kaustubh.transactions.common.api.CreateTransactionResponse;
import com.kaustubh.transactions.common.enums.TransactionType;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionIngressService transactionIngressService;

    @InjectMocks
    private TransactionController transactionController;

    @Test
    void createTransaction_returnsAcceptedResponse() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                "idem-123",
                "acc-123",
                new BigDecimal("100.00"),
                "INR",
                TransactionType.CREDIT,
                "https://merchant.example/webhook"
        );

        CreateTransactionResponse serviceResponse = new CreateTransactionResponse(
                "tx-123",
                "ACCEPTED"
        );

        when(transactionIngressService.accept(request, "merchant-demo")).thenReturn(serviceResponse);

        ResponseEntity<CreateTransactionResponse> response =
                transactionController.createTransaction("merchant-demo", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
        verify(transactionIngressService).accept(request, "merchant-demo");
    }
}
