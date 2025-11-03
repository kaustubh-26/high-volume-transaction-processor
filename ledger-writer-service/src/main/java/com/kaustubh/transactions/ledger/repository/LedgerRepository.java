package com.kaustubh.transactions.ledger.repository;

import java.sql.PreparedStatement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.config.LedgerBatchProperties;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LedgerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final LedgerBatchProperties ledgerBatchProperties;

    public int[][] batchInsert(List<TransactionLogEvent> events) {
        String sql = """
                INSERT INTO ledger_entries
                (transaction_id, idempotency_key, account_id, amount, currency, type, status, processed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (transaction_id) DO NOTHING
                """;

        return jdbcTemplate.batchUpdate(
                sql,
                events,
                ledgerBatchProperties.batchSize(),
                (PreparedStatement ps, TransactionLogEvent event) -> {
                    ps.setString(1, event.transactionId());
                    ps.setString(2, event.idempotencyKey());
                    ps.setString(3, event.merchantId());
                    ps.setString(4, event.accountId());
                    ps.setBigDecimal(5, event.amount());
                    ps.setString(6, event.currency());
                    ps.setString(7, event.type().name());
                    ps.setString(8, TransactionStatus.PERSISTED.name());
                    ps.setTimestamp(9, java.sql.Timestamp.from(event.processedAt()));
                }
        );
    }
}
