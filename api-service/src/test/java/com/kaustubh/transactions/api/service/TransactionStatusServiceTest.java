package com.kaustubh.transactions.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kaustubh.transactions.api.repository.TransactionStatusRepository;
import com.kaustubh.transactions.api.repository.TransactionStatusRepository.TransactionStatusRecord;
import com.kaustubh.transactions.common.api.TransactionStatusResponse;

@ExtendWith(MockitoExtension.class)
class TransactionStatusServiceTest {

    @Mock
    private TransactionStatusRepository transactionStatusRepository;

    @Test
    void findStatus_returnsRepositoryResult() {
        TransactionStatusService service = new TransactionStatusService(transactionStatusRepository);
        TransactionStatusRecord statusRecord = new TransactionStatusRecord(
                "tx-1",
                "merchant-1",
                "PERSISTED",
                Instant.parse("2026-03-22T11:00:00Z")
        );

        when(transactionStatusRepository.findStatusByTransactionId("tx-1"))
                .thenReturn(Optional.of(statusRecord));

        assertThat(service.findStatus("tx-1")).contains(statusRecord);
        TransactionStatusResponse response = service.buildResponse(statusRecord).orElseThrow();
        assertThat(response.transactionId()).isEqualTo("tx-1");
        assertThat(response.status()).isEqualTo("PERSISTED");
        assertThat(response.processedAt()).isEqualTo(Instant.parse("2026-03-22T11:00:00Z"));
    }

    @Test
    void buildResponse_returnsEmptyWhenStatusRecordNull() {
        TransactionStatusService service = new TransactionStatusService(transactionStatusRepository);

        assertThat(service.buildResponse(null)).isEmpty();
    }
}
