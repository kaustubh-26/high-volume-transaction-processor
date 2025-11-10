package com.kaustubh.transactions.processor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;

@ExtendWith(MockitoExtension.class)
class TransactionProcessingServiceTest {

    @Mock
    private RedisIdempotencyStore redisIdempotencyStore;

    private TransactionProcessingService service;

    @BeforeEach
    void setUp() {
        service = new TransactionProcessingService(redisIdempotencyStore);
    }

    @Test
    void process_returnsProcessedResultWhenIdempotencyAcquired() {
        TransactionRequestEvent event = validEvent();

        when(redisIdempotencyStore.acquire("idem-1", "tx-1")).thenReturn(true);

        ProcessingResult result = service.process(event);

        assertThat(result.duplicate()).isFalse();

        TransactionLogEvent logEvent = result.transactionLogEvent();
        assertThat(logEvent).isNotNull();
        assertThat(logEvent.transactionId()).isEqualTo(event.transactionId());
        assertThat(logEvent.idempotencyKey()).isEqualTo(event.idempotencyKey());
        assertThat(logEvent.accountId()).isEqualTo(event.accountId());
        assertThat(logEvent.amount()).isEqualTo(event.amount());
        assertThat(logEvent.currency()).isEqualTo(event.currency());
        assertThat(logEvent.type()).isEqualTo(event.type());
        assertThat(logEvent.correlationId()).isEqualTo(event.correlationId());
        assertThat(logEvent.status()).isEqualTo(TransactionStatus.ACCEPTED);
        assertThat(logEvent.eventId()).isNotNull();
        assertThat(logEvent.processedAt()).isNotNull();

        verify(redisIdempotencyStore).acquire("idem-1", "tx-1");
        verify(redisIdempotencyStore, never()).getExistingTransactionId(anyString());
    }

    @Test
    void process_returnsDuplicateResultWhenIdempotencyRejected() {
        TransactionRequestEvent event = validEvent();

        when(redisIdempotencyStore.acquire("idem-1", "tx-1")).thenReturn(false);
        when(redisIdempotencyStore.getExistingTransactionId("idem-1")).thenReturn("tx-existing");

        ProcessingResult result = service.process(event);

        assertThat(result.duplicate()).isTrue();
        assertThat(result.transactionLogEvent()).isNull();
        verify(redisIdempotencyStore).getExistingTransactionId("idem-1");
    }

    @Test
    void process_throwsForInvalidAmount() {
        TransactionRequestEvent event = new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "acct-1",
                new BigDecimal("0.00"),
                "USD",
                TransactionType.DEBIT,
                null,
                "corr-1",
                Instant.parse("2024-01-01T00:00:00Z")
        );

        assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must be greater than zero");
    }

    @Test
    void process_throwsForBlankAccountId() {
        TransactionRequestEvent event = new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "   ",
                new BigDecimal("10.00"),
                "USD",
                TransactionType.CREDIT,
                null,
                "corr-1",
                Instant.parse("2024-01-01T00:00:00Z")
        );

        assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("accountId must not be blank");
    }

    private TransactionRequestEvent validEvent() {
        return new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "acct-1",
                new BigDecimal("10.00"),
                "USD",
                TransactionType.DEBIT,
                null,
                "corr-1",
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }
}
