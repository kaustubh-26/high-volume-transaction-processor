package com.kaustubh.transactions.ledger.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kaustubh.transactions.ledger.config.ReconciliationProperties;

@ExtendWith(MockitoExtension.class)
class LedgerReconciliationJobTest {

    @Mock
    private LedgerReconciliationService reconciliationService;

    @Test
    void run_skipsReconciliationWhenDisabled() {
        LedgerReconciliationJob job = new LedgerReconciliationJob(
                reconciliationService,
                new ReconciliationProperties(false, 60000, 30)
        );

        job.run();

        verifyNoInteractions(reconciliationService);
    }

    @Test
    void run_reconcilesConfiguredLookbackWindowWhenEnabled() {
        LedgerReconciliationJob job = new LedgerReconciliationJob(
                reconciliationService,
                new ReconciliationProperties(true, 60000, 45)
        );

        job.run();

        verify(reconciliationService).reconcileWindow(45L);
    }
}
