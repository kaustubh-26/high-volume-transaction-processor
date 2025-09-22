package com.kaustubh.transactions.ledger.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LedgerReconciliationJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public long countLedgerEntriesSince(Instant since) {
        String sql = """
                SELECT COUNT(*)
                FROM ledger_entries
                WHERE processed_at >= ?
                """;

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                Timestamp.from(since)
        );

        return count == null ? 0 : count;
    }

    public List<String> findRecentLedgerTransactionIdsSince(Instant since) {
        String sql = """
                SELECT transaction_id
                FROM ledger_entries
                WHERE processed_at >= ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString("transaction_id"),
                Timestamp.from(since)
        );
    }

    public void insertReconciliationRun(
            Instant runAt,
            Instant windowStart,
            long auditCount,
            long ledgerCount,
            long missingInLedger,
            String status,
            String notes
    ) {
        String sql = """
                INSERT INTO reconciliation_runs
                (run_at, window_start, audit_count, ledger_count, missing_in_ledger, status, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                Timestamp.from(runAt),
                Timestamp.from(windowStart),
                auditCount,
                ledgerCount,
                missingInLedger,
                status,
                notes
        );
    }
}