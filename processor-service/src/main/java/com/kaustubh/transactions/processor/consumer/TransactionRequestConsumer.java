package com.kaustubh.transactions.processor.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.processor.service.ProcessingResult;
import com.kaustubh.transactions.processor.service.TransactionLogPublisher;
import com.kaustubh.transactions.processor.service.TransactionProcessingService;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRequestConsumer {

    private final TransactionProcessingService transactionProcessingService;
    private final TransactionLogPublisher transactionLogPublisher;
    private final TransactionWebhookNotifier webhookNotifier;

    @KafkaListener(topics = "${app.kafka.topic.transaction-requests}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TransactionRequestEvent event, Acknowledgment acknowledgement) {
        try {
            ProcessingResult result = transactionProcessingService.process(event);

            if (result.duplicate()) {
                webhookNotifier.sendStatusUpdate(
                        event.callbackUrl(),
                        TransactionStatus.REJECTED,
                        event.transactionId(),
                        event.correlationId(),
                        event.createdAt()
                );
                acknowledgement.acknowledge();
                log.info(
                        "Acknowledged duplicate transaction request transactionId={} eventId={}",
                        event.transactionId(),
                        event.eventId());
                return;
            }

            transactionLogPublisher.publish(result.transactionLogEvent());
            webhookNotifier.sendStatusUpdate(
                    result.transactionLogEvent().callbackUrl(),
                    TransactionStatus.ACCEPTED,
                    result.transactionLogEvent().transactionId(),
                    result.transactionLogEvent().correlationId(),
                    result.transactionLogEvent().processedAt()
            );

            acknowledgement.acknowledge();

            log.info(
                    "Acknowledged processed transaction request transactionId={} eventId={}",
                    event.transactionId(),
                    event.eventId(),
                    result.transactionLogEvent().eventId()

            );
        } catch (Exception ex) {
            log.error(
                    "Failed to process transaction request transactionId={} eventId={}",
                    event.transactionId(),
                    event.eventId(),
                    ex);
            throw ex;
        }
    }
}
