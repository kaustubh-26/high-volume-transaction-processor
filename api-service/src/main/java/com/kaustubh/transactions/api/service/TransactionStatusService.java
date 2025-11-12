package com.kaustubh.transactions.api.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kaustubh.transactions.api.repository.TransactionStatusRepository;
import com.kaustubh.transactions.common.api.TransactionStatusResponse;
import com.kaustubh.transactions.api.repository.TransactionStatusRepository.TransactionStatusRecord;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionStatusService {

    private final TransactionStatusRepository transactionStatusRepository;

    public Optional<TransactionStatusRecord> findStatus(String transactionId) {
        return transactionStatusRepository.findStatusByTransactionId(transactionId);
    }

    public Optional<TransactionStatusResponse> buildResponse(TransactionStatusRecord statusRecord) {
        if (statusRecord == null) {
            return Optional.empty();
        }
        return Optional.of(new TransactionStatusResponse(
                statusRecord.transactionId(),
                statusRecord.status(),
                statusRecord.processedAt()
        ));
    }
}
