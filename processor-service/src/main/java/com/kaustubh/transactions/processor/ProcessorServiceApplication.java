package com.kaustubh.transactions.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.kaustubh.transactions.processor.config.IdempotencyProperties;

@SpringBootApplication
@EnableConfigurationProperties(IdempotencyProperties.class)
public class ProcessorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcessorServiceApplication.class, args);
    }
}
