package com.kaustubh.transactions.ledger.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuditEventMongoRepository {

    private final MongoTemplate mongoTemplate;

    public long countAuditEventsSince(Instant since) {
        Query query = new Query();
        query.addCriteria(Criteria.where("processedAt").gte(since));
        return mongoTemplate.count(query, AuditEventDocument.class);
    }

    public List<String> findRecentAuditTransactionIdsSince(Instant since) {
        Query query = new Query();
        query.addCriteria(Criteria.where("processedAt").gte(since));
        query.fields().include("transactionId");
        return mongoTemplate.find(query, AuditEventDocument.class)
                .stream()
                .map(AuditEventDocument::transactionId)
                .toList();
    }
}