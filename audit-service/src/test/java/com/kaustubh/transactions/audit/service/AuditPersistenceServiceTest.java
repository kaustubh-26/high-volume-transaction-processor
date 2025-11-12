package com.kaustubh.transactions.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kaustubh.transactions.audit.document.TransactionAuditEventDocument;
import com.kaustubh.transactions.audit.repository.TransactionAuditEventRepository;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;

@ExtendWith(MockitoExtension.class)
class AuditPersistenceServiceTest {

    @Mock
    private TransactionAuditEventRepository repository;

    private AuditPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new AuditPersistenceService(repository);
    }

    @Test
    void persist_buildsDocumentFromEvent() {
        TransactionLogEvent event = new TransactionLogEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "acct-1",
                new BigDecimal("12.34"),
                "USD",
                TransactionType.CREDIT,
                TransactionStatus.ACCEPTED,
                null,
                "corr-1",
                Instant.parse("2024-01-01T00:00:00Z")
        );

        service.persist(event, "transaction-log");

        ArgumentCaptor<TransactionAuditEventDocument> captor = ArgumentCaptor.forClass(TransactionAuditEventDocument.class);
        verify(repository).save(captor.capture());

        TransactionAuditEventDocument document = captor.getValue();
        assertThat(document.id()).isNotBlank();
        assertThat(document.eventId()).isEqualTo(event.eventId());
        assertThat(document.transactionId()).isEqualTo(event.transactionId());
        assertThat(document.idempotencyKey()).isEqualTo(event.idempotencyKey());
        assertThat(document.merchantId()).isEqualTo(event.merchantId());
        assertThat(document.accountId()).isEqualTo(event.accountId());
        assertThat(document.correlationId()).isEqualTo(event.correlationId());
        assertThat(document.amount()).isEqualTo(event.amount());
        assertThat(document.currency()).isEqualTo(event.currency());
        assertThat(document.type()).isEqualTo(event.type().name());
        assertThat(document.status()).isEqualTo(event.status().name());
        assertThat(document.processedAt()).isEqualTo(event.processedAt());
        assertThat(document.storedAt()).isNotNull();
        assertThat(document.sourceTopic()).isEqualTo("transaction-log");
    }
}
