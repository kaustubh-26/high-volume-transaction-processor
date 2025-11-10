package com.kaustubh.transactions.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import com.kaustubh.transactions.common.api.CreateTransactionRequest;
import com.kaustubh.transactions.common.api.CreateTransactionResponse;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;

@ExtendWith(MockitoExtension.class)
class TransactionIngressServiceTest {

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @Test
    void accept_publishesEventAndReturnsResponse() {
        TransactionIngressService service = new TransactionIngressService(transactionEventPublisher);

        MDC.put("correlationId", "corr-123");
        try {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    "idem-1",
                    "acct-1",
                    new BigDecimal("50.00"),
                    "USD",
                    TransactionType.DEBIT,
                    "https://merchant.example/webhook"
            );

            CreateTransactionResponse response = service.accept(request, "merchant-demo");

            ArgumentCaptor<TransactionRequestEvent> captor = ArgumentCaptor.forClass(TransactionRequestEvent.class);
            verify(transactionEventPublisher).publish(captor.capture());

            TransactionRequestEvent event = captor.getValue();

            assertThat(response.transactionId()).isEqualTo(event.transactionId());
            assertThat(response.status()).isEqualTo(TransactionStatus.RECEIVED.name());

            assertThat(event.eventId()).isNotNull();
            assertThat(event.transactionId()).isNotBlank();
            assertThat(event.createdAt()).isNotNull();
            assertThat(event.correlationId()).isEqualTo("corr-123");

            assertThat(event.idempotencyKey()).isEqualTo(request.idempotencyKey());
            assertThat(event.merchantId()).isEqualTo("merchant-demo");
            assertThat(event.accountId()).isEqualTo(request.accountId());
            assertThat(event.amount()).isEqualTo(request.amount());
            assertThat(event.currency()).isEqualTo(request.currency());
            assertThat(event.type()).isEqualTo(request.type());
            assertThat(event.callbackUrl()).isEqualTo(request.callbackUrl());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
