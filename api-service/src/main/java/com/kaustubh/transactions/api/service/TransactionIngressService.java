package com.kaustubh.transactions.api.service;

import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.api.CreateTransactionRequest;
import com.kaustubh.transactions.common.api.CreateTransactionResponse;
import com.kaustubh.transactions.common.util.IdGenerator;

@Service
public class TransactionIngressService {

    public CreateTransactionResponse accept(CreateTransactionRequest request) {
        String transactionId = IdGenerator.newTransactionId();

        return new CreateTransactionResponse(
            transactionId,
            "ACCEPTED"
        );
    }
}
