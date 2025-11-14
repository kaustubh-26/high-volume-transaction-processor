package com.kaustubh.transactions.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.kaustubh.transactions.api.config.KafkaTopicProperties;
import com.kaustubh.transactions.api.config.MerchantProperties;
import com.kaustubh.transactions.api.config.ReadAuthProperties;

@SpringBootApplication
@EnableConfigurationProperties({ KafkaTopicProperties.class, MerchantProperties.class, ReadAuthProperties.class })
public class ApiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiServiceApplication.class, args);
    }
}
