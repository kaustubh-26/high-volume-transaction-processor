package com.kaustubh.transactions.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.kaustubh.transactions.api.repository.TransactionStatusRepository.TransactionStatusRecord;

@ExtendWith(MockitoExtension.class)
class TransactionStatusRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    @Test
    void findStatusByTransactionId_mapsTimestampToInstant() throws Exception {
        TransactionStatusRepository repository = new TransactionStatusRepository(jdbcTemplate);
        Instant processedAt = Instant.parse("2026-03-22T10:15:30Z");
        when(jdbcTemplate.queryForObject(any(String.class), any(RowMapper.class), eq("tx-1")))
                .thenAnswer(invocation -> {
                    RowMapper<TransactionStatusRecord> mapper = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.getString("transaction_id")).thenReturn("tx-1");
                    when(resultSet.getString("merchant_id")).thenReturn("merchant-1");
                    when(resultSet.getString("status")).thenReturn("PERSISTED");
                    when(resultSet.getTimestamp("processed_at")).thenReturn(Timestamp.from(processedAt));
                    return mapper.mapRow(resultSet, 0);
                });

        Optional<TransactionStatusRecord> response = repository.findStatusByTransactionId("tx-1");

        assertThat(response)
                .contains(new TransactionStatusRecord("tx-1", "merchant-1", "PERSISTED", processedAt));
    }

    @SuppressWarnings("unchecked")
    @Test
    void findStatusByTransactionId_mapsNullProcessedAt() throws Exception {
        TransactionStatusRepository repository = new TransactionStatusRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(any(String.class), any(RowMapper.class), eq("tx-2")))
                .thenAnswer(invocation -> {
                    RowMapper<TransactionStatusRecord> mapper = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.getString("transaction_id")).thenReturn("tx-2");
                    when(resultSet.getString("merchant_id")).thenReturn("merchant-2");
                    when(resultSet.getString("status")).thenReturn("FAILED");
                    when(resultSet.getTimestamp("processed_at")).thenReturn(null);
                    return mapper.mapRow(resultSet, 0);
                });

        Optional<TransactionStatusRecord> response = repository.findStatusByTransactionId("tx-2");

        assertThat(response)
                .contains(new TransactionStatusRecord("tx-2", "merchant-2", "FAILED", null));
    }

    @SuppressWarnings("unchecked")
    @Test
    void findStatusByTransactionId_returnsEmptyWhenJdbcReturnsNoRows() {
        TransactionStatusRepository repository = new TransactionStatusRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(any(String.class), any(RowMapper.class), eq("missing-tx")))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThat(repository.findStatusByTransactionId("missing-tx")).isEmpty();
    }
}
