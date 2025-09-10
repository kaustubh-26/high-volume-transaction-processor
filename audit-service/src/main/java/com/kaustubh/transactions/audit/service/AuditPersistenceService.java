package com.kaustubh.transactions.audit.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kaustubh.transactions.audit.document.TransactionAuditEventDocument;
import com.kaustubh.transactions.audit.repository.TransactionAuditEventRepository;
import com.kaustubh.transactions.common.event.TransactionLogEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditPersistenceService {

    private final TransactionAuditEventRepository repository;

    public void persist(TransactionLogEvent event, String sourceTopic) {
        TransactionAuditEventDocument document = new TransactionAuditEventDocument(
                UUID.randomUUID().toString(),
                event.eventId(),
                event.transactionId(),
                event.idempotencyKey(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.type().name(),
                event.status().name(),
                event.processedAt(),
                Instant.now(),
                sourceTopic);

        repository.save(document);

        log.info(
                "Audit event persisted transactionId={} eventId={} sourceTopic={}",
                event.transactionId(),
                event.eventId(),
                sourceTopic);
    }
}
