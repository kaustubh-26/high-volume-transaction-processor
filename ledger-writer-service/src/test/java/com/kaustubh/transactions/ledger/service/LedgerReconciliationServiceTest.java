package com.kaustubh.transactions.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kaustubh.transactions.ledger.repository.AuditEventMongoRepository;
import com.kaustubh.transactions.ledger.repository.LedgerReconciliationJdbcRepository;

@ExtendWith(MockitoExtension.class)
class LedgerReconciliationServiceTest {

    @Mock
    private LedgerReconciliationJdbcRepository ledgerRepository;

    @Mock
    private AuditEventMongoRepository auditRepository;

    private LedgerReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new LedgerReconciliationService(ledgerRepository, auditRepository);
    }

    @Test
    void reconcileWindow_recordsOkStatusWhenLedgerMatchesAudit() {
        when(auditRepository.countAuditEventsSince(any())).thenReturn(2L);
        when(ledgerRepository.countLedgerEntriesSince(any())).thenReturn(2L);
        when(auditRepository.findRecentAuditTransactionIdsSince(any())).thenReturn(List.of("tx-1", "tx-2"));
        when(ledgerRepository.findRecentLedgerTransactionIdsSince(any())).thenReturn(List.of("tx-2", "tx-1"));

        service.reconcileWindow(15);

        ArgumentCaptor<Instant> windowStartCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(auditRepository).countAuditEventsSince(windowStartCaptor.capture());
        Instant windowStart = windowStartCaptor.getValue();

        verify(ledgerRepository).countLedgerEntriesSince(windowStart);
        verify(auditRepository).findRecentAuditTransactionIdsSince(windowStart);
        verify(ledgerRepository).findRecentLedgerTransactionIdsSince(windowStart);

        ArgumentCaptor<Instant> runAtCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> insertWindowStartCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Long> auditCountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> ledgerCountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> missingCountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> notesCaptor = ArgumentCaptor.forClass(String.class);
        verify(ledgerRepository).insertReconciliationRun(
                runAtCaptor.capture(),
                insertWindowStartCaptor.capture(),
                auditCountCaptor.capture(),
                ledgerCountCaptor.capture(),
                missingCountCaptor.capture(),
                statusCaptor.capture(),
                notesCaptor.capture()
        );

        Instant runAt = runAtCaptor.getValue();

        assertThat(insertWindowStartCaptor.getValue()).isEqualTo(windowStart);
        assertThat(windowStart).isEqualTo(runAt.minus(15, ChronoUnit.MINUTES));
        assertThat(auditCountCaptor.getValue()).isEqualTo(2L);
        assertThat(ledgerCountCaptor.getValue()).isEqualTo(2L);
        assertThat(missingCountCaptor.getValue()).isZero();
        assertThat(statusCaptor.getValue()).isEqualTo("OK");
        assertThat(notesCaptor.getValue()).isEqualTo("Ledger matches audit window");
    }

    @Test
    void reconcileWindow_recordsDriftStatusAndLimitsMissingIdsInNotes() {
        List<String> auditTransactionIds = IntStream.rangeClosed(1, 25)
                .mapToObj(index -> "tx-" + index)
                .toList();

        when(auditRepository.countAuditEventsSince(any())).thenReturn(25L);
        when(ledgerRepository.countLedgerEntriesSince(any())).thenReturn(0L);
        when(auditRepository.findRecentAuditTransactionIdsSince(any())).thenReturn(auditTransactionIds);
        when(ledgerRepository.findRecentLedgerTransactionIdsSince(any())).thenReturn(List.of());

        service.reconcileWindow(30);

        ArgumentCaptor<Long> missingCountCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> notesCaptor = ArgumentCaptor.forClass(String.class);
        verify(ledgerRepository).insertReconciliationRun(
                any(Instant.class),
                any(Instant.class),
                anyLong(),
                anyLong(),
                missingCountCaptor.capture(),
                statusCaptor.capture(),
                notesCaptor.capture()
        );

        assertThat(missingCountCaptor.getValue()).isEqualTo(25L);
        assertThat(statusCaptor.getValue()).isEqualTo("DRIFT_DETECTED");
        assertThat(notesCaptor.getValue()).startsWith("Missing transactionIds in ledger: ");

        String idsPortion = notesCaptor.getValue().substring("Missing transactionIds in ledger: ".length());
        List<String> idsInNotes = List.of(idsPortion.split(","));

        assertThat(idsInNotes)
                .hasSize(20)
                .allMatch(auditTransactionIds::contains);
    }
}
