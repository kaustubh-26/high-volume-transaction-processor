package com.kaustubh.transactions.ledger.service;

import com.kaustubh.transactions.ledger.repository.AuditEventMongoRepository;
import com.kaustubh.transactions.ledger.repository.LedgerReconciliationJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerReconciliationService {

    private final LedgerReconciliationJdbcRepository ledgerRepository;
    private final AuditEventMongoRepository auditRepository;

    public void reconcileWindow(long lookbackMinutes) {
        Instant runAt = Instant.now();
        Instant windowStart = runAt.minus(lookbackMinutes, ChronoUnit.MINUTES);

        long auditCount = auditRepository.countAuditEventsSince(windowStart);
        long ledgerCount = ledgerRepository.countLedgerEntriesSince(windowStart);

        List<String> auditTransactionIds = auditRepository.findRecentAuditTransactionIdsSince(windowStart);
        List<String> ledgerTransactionIds = ledgerRepository.findRecentLedgerTransactionIdsSince(windowStart);

        Set<String> missingInLedger = new HashSet<>(auditTransactionIds);
        missingInLedger.removeAll(new HashSet<>(ledgerTransactionIds));

        String status = missingInLedger.isEmpty() ? "OK" : "DRIFT_DETECTED";
        String notes = missingInLedger.isEmpty()
                ? "Ledger matches audit window"
                : "Missing transactionIds in ledger: " + String.join(",", missingInLedger.stream().limit(20).toList());

        ledgerRepository.insertReconciliationRun(
                runAt,
                windowStart,
                auditCount,
                ledgerCount,
                missingInLedger.size(),
                status,
                notes
        );

        if (missingInLedger.isEmpty()) {
            log.info(
                    "Reconciliation passed windowStart={} auditCount={} ledgerCount={}",
                    windowStart,
                    auditCount,
                    ledgerCount
            );
            return;
        }

        log.warn(
                "Reconciliation drift detected windowStart={} auditCount={} ledgerCount={} missingInLedger={}",
                windowStart,
                auditCount,
                ledgerCount,
                missingInLedger
        );
    }
}
