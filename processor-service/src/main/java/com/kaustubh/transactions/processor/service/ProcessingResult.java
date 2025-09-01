package com.kaustubh.transactions.processor.service;

import com.kaustubh.transactions.common.event.TransactionLogEvent;

public record ProcessingResult(
        boolean duplicate,
        TransactionLogEvent transactionLogEvent
) {
    public static ProcessingResult duplicateResult() {
        return new ProcessingResult(true, null);
    }

    public static ProcessingResult processed(TransactionLogEvent event) {
        return new ProcessingResult(false, event);
    }
}