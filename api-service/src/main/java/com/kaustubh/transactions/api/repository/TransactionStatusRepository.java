package com.kaustubh.transactions.api.repository;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TransactionStatusRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<TransactionStatusRecord> findStatusByTransactionId(String transactionId) {
        String sql = """
                SELECT transaction_id, merchant_id, status, processed_at
                FROM ledger_entries
                WHERE transaction_id = ?
                """;

        try {
            TransactionStatusRecord response = jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new TransactionStatusRecord(
                            rs.getString("transaction_id"),
                            rs.getString("merchant_id"),
                            rs.getString("status"),
                            Optional.ofNullable(rs.getTimestamp("processed_at"))
                                    .map(Timestamp::toInstant)
                                    .orElse(null)
                    ),
                    transactionId
            );
            return Optional.ofNullable(response);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public record TransactionStatusRecord(
            String transactionId,
            String merchantId,
            String status,
            java.time.Instant processedAt
    ) {
    }
}
