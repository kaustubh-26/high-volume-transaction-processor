package com.kaustubh.transactions.ledger.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class AuditEventMongoRepositoryTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void countAuditEventsSince_buildsProcessedAtLowerBoundQuery() {
        AuditEventMongoRepository repository = new AuditEventMongoRepository(mongoTemplate);
        Instant since = Instant.parse("2026-03-22T10:15:30Z");
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(mongoTemplate.count(any(Query.class), eq(AuditEventDocument.class))).thenReturn(7L);

        long count = repository.countAuditEventsSince(since);

        assertThat(count).isEqualTo(7L);
        verify(mongoTemplate).count(queryCaptor.capture(), eq(AuditEventDocument.class));
        Document processedAtCriteria = (Document) queryCaptor.getValue().getQueryObject().get("processedAt");
        assertThat(processedAtCriteria).hasSize(1);
        assertThat(processedAtCriteria.values()).containsExactly(since);
    }

    @Test
    void findRecentAuditTransactionIdsSince_returnsTransactionIds() {
        AuditEventMongoRepository repository = new AuditEventMongoRepository(mongoTemplate);
        Instant since = Instant.parse("2026-03-22T10:15:30Z");
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        when(mongoTemplate.find(any(Query.class), eq(AuditEventDocument.class))).thenReturn(List.of(
                auditEventDocument("tx-1"),
                auditEventDocument("tx-2")
        ));

        List<String> transactionIds = repository.findRecentAuditTransactionIdsSince(since);

        assertThat(transactionIds).containsExactly("tx-1", "tx-2");
        verify(mongoTemplate).find(queryCaptor.capture(), eq(AuditEventDocument.class));
        Document processedAtCriteria = (Document) queryCaptor.getValue().getQueryObject().get("processedAt");
        assertThat(processedAtCriteria).hasSize(1);
        assertThat(processedAtCriteria.values()).containsExactly(since);
        assertThat(queryCaptor.getValue().getFieldsObject())
                .containsEntry("transactionId", 1);
    }

    private AuditEventDocument auditEventDocument(String transactionId) {
        return new AuditEventDocument(
                "doc-" + transactionId,
                UUID.randomUUID(),
                transactionId,
                "idem-1",
                "acct-1",
                new BigDecimal("10.00"),
                "INR",
                "CREDIT",
                "PERSISTED",
                Instant.parse("2026-03-22T10:15:30Z"),
                Instant.parse("2026-03-22T10:16:00Z"),
                "transaction_log"
        );
    }
}
