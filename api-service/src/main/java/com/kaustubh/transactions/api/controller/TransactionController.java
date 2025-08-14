package com.kaustubh.transactions.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaustubh.transactions.api.service.TransactionIngressService;
import com.kaustubh.transactions.common.api.CreateTransactionRequest;
import com.kaustubh.transactions.common.api.CreateTransactionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionIngressService transactionIngressService;

    @PostMapping
    public ResponseEntity<CreateTransactionResponse> createTransaction(
        @Valid @RequestBody CreateTransactionRequest request) {

            CreateTransactionResponse response = transactionIngressService.accept(request);
            
            return ResponseEntity.accepted().body(response);
        }
}
