package com.kaustubh.transactions.processor.service;

import com.kaustubh.transactions.common.event.TransactionLogEvent;

public record ProcessingResult(
        Outcome outcome,
        TransactionLogEvent transactionLogEvent
) {

    public enum Outcome {
        PROCESSED,
        DUPLICATE_REJECTED,
        DUPLICATE_REPLAY
    }

    public boolean duplicate() {
        return outcome != Outcome.PROCESSED;
    }

    public boolean duplicateRejected() {
        return outcome == Outcome.DUPLICATE_REJECTED;
    }

    public boolean duplicateReplay() {
        return outcome == Outcome.DUPLICATE_REPLAY;
    }

    public static ProcessingResult duplicateRejectedResult() {
        return new ProcessingResult(Outcome.DUPLICATE_REJECTED, null);
    }

    public static ProcessingResult duplicateReplayResult() {
        return new ProcessingResult(Outcome.DUPLICATE_REPLAY, null);
    }

    public static ProcessingResult processed(TransactionLogEvent event) {
        return new ProcessingResult(Outcome.PROCESSED, event);
    }
}
