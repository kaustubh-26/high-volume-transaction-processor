package com.kaustubh.transactions.audit.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.kaustubh.transactions.audit.document.TransactionAuditEventDocument;

public interface TransactionAuditEventRepository extends
        MongoRepository<TransactionAuditEventDocument, String> {
}