package com.kaustubh.transactions.ledger.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.repository.LedgerRepository;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

@ExtendWith(MockitoExtension.class)
class LedgerPersistenceServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionWebhookNotifier webhookNotifier;

    private LedgerPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new LedgerPersistenceService(ledgerRepository, webhookNotifier);
    }

    @Test
    void persistBatch_skipsNullOrEmpty() {
        service.persistBatch(null);
        service.persistBatch(List.of());

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void persistBatch_callsRepositoryForEvents() {
        List<TransactionLogEvent> events = List.of(logEvent("tx-1"), logEvent("tx-2"));
        when(ledgerRepository.batchInsert(events)).thenReturn(new int[][] {{1, 0}, {1}});

        service.persistBatch(events);

        verify(ledgerRepository).batchInsert(events);
    }

    private TransactionLogEvent logEvent(String transactionId) {
        return new TransactionLogEvent(
                UUID.randomUUID(),
                transactionId,
                "idem-1",
                "acct-1",
                new BigDecimal("10.00"),
                "USD",
                TransactionType.DEBIT,
                TransactionStatus.ACCEPTED,
                null,
                "corr-1",
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }
}
