package com.kaustubh.transactions.processor.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.processor.service.TransactionProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRequestConsumer {
    
    private final TransactionProcessingService transactionProcessingService;

    @KafkaListener(
        topics = "${app.kafka.topic.transaction-requests}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(TransactionRequestEvent event, Acknowledgment acknowledgement) {
        try {
            transactionProcessingService.process(event);
            acknowledgement.acknowledge();

            log.info(
                "Acknowledged transaction request transactionId={} eventId={}",
                event.transactionId(),
                event.eventId()
            );
        } catch (Exception ex) {
            log.error(
                "Failed to process transaction request transactionId={} eventId={}",
                event.transactionId(),
                event.eventId(),
                ex
            );
            throw ex;
        }
    }
}
