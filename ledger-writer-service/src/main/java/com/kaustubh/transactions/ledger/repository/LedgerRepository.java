package com.kaustubh.transactions.ledger.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kaustubh.transactions.common.event.TransactionLogEvent;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LedgerRepository {

    private final JdbcTemplate jdbcTemplate;

    public int insert(TransactionLogEvent event) {
        String sql = """
                INSERT INTO ledger_entries
                (transaction_id, idempotency_key, account_id, amount, currency, type, status, processed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (transaction_id) DO NOTHING
                """;

        return jdbcTemplate.update(
                sql,
                event.transactionId(),
                event.idempotencyKey(),
                event.accountId(),
                event.amount(),
                event.currency(),
                event.type().name(),
                event.status().name(),
                java.sql.Timestamp.from(event.processedAt())
        );
    }
}
