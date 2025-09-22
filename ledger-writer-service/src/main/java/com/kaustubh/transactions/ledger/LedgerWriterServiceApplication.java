package com.kaustubh.transactions.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.kaustubh.transactions.ledger.config.LedgerBatchProperties;
import com.kaustubh.transactions.ledger.config.ProcessorKafkaListenerProperties;
import com.kaustubh.transactions.ledger.config.ReconciliationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        LedgerBatchProperties.class,
        ProcessorKafkaListenerProperties.class,
        ReconciliationProperties.class
})
public class LedgerWriterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerWriterServiceApplication.class, args);
    }
}