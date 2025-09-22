package com.kaustubh.transactions.ledger.service;

import com.kaustubh.transactions.ledger.config.ReconciliationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerReconciliationJob {

    private final LedgerReconciliationService reconciliationService;
    private final ReconciliationProperties reconciliationProperties;

    @Scheduled(fixedDelayString = "${app.reconciliation.fixed-delay-ms}")
    public void run() {
        if (!reconciliationProperties.enabled()) {
            log.info("Reconciliation job disabled");
            return;
        }

        reconciliationService.reconcileWindow(reconciliationProperties.lookbackMinutes());
    }
}