package com.kaustubh.transactions.ledger.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.service.LedgerPersistenceService;

@ExtendWith(MockitoExtension.class)
class TransactionLogConsumerTest {

    @Mock
    private LedgerPersistenceService ledgerPersistenceService;

    @Mock
    private Acknowledgment acknowledgment;

    private TransactionLogConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransactionLogConsumer(ledgerPersistenceService);
    }

    @Test
    void consume_skipsNullBatch() {
        consumer.consume(null, acknowledgment);

        verify(ledgerPersistenceService, never()).persistBatch(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consume_skipsEmptyBatch() {
        consumer.consume(List.of(), acknowledgment);

        verify(ledgerPersistenceService, never()).persistBatch(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consume_persistsAndAcknowledgesBatch() {
        List<TransactionLogEvent> events = List.of(logEvent("tx-1"), logEvent("tx-2"));

        consumer.consume(events, acknowledgment);

        InOrder inOrder = inOrder(ledgerPersistenceService, acknowledgment);
        inOrder.verify(ledgerPersistenceService).persistBatch(events);
        inOrder.verify(acknowledgment).acknowledge();
    }

    @Test
    void consume_propagatesExceptions() {
        List<TransactionLogEvent> events = List.of(logEvent("tx-1"));
        doThrow(new IllegalStateException("boom")).when(ledgerPersistenceService).persistBatch(events);

        assertThatThrownBy(() -> consumer.consume(events, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        verify(acknowledgment, never()).acknowledge();
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
                "corr-1",
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }
}
