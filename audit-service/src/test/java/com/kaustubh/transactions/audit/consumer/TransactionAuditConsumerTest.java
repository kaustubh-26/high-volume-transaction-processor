package com.kaustubh.transactions.audit.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.kaustubh.transactions.audit.service.AuditPersistenceService;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;

@ExtendWith(MockitoExtension.class)
class TransactionAuditConsumerTest {

    @Mock
    private AuditPersistenceService auditPersistenceService;

    @Mock
    private Acknowledgment acknowledgment;

    private TransactionAuditConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TransactionAuditConsumer(auditPersistenceService);
    }

    @Test
    void consume_persistsAndAcknowledges() {
        TransactionLogEvent event = logEvent();

        consumer.consume(event, "transaction-log", acknowledgment);

        InOrder inOrder = inOrder(auditPersistenceService, acknowledgment);
        inOrder.verify(auditPersistenceService).persist(event, "transaction-log");
        inOrder.verify(acknowledgment).acknowledge();
    }

    @Test
    void consume_propagatesExceptions() {
        TransactionLogEvent event = logEvent();
        doThrow(new IllegalStateException("boom"))
                .when(auditPersistenceService)
                .persist(event, "transaction-log");

        assertThatThrownBy(() -> consumer.consume(event, "transaction-log", acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        verify(acknowledgment, never()).acknowledge();
    }

    private TransactionLogEvent logEvent() {
        return new TransactionLogEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
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
