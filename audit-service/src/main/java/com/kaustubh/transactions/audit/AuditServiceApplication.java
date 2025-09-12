package com.kaustubh.transactions.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.kaustubh.transactions.audit.config.ProcessorKafkaListenerProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProcessorKafkaListenerProperties.class)
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
