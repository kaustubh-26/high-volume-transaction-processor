package com.kaustubh.transactions.processor.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.processor.service.ProcessingResult;
import com.kaustubh.transactions.processor.service.TransactionLogPublisher;
import com.kaustubh.transactions.processor.service.TransactionProcessingService;
import com.kaustubh.transactions.processor.service.WebhookDispatchPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRequestConsumer {

    private final TransactionProcessingService transactionProcessingService;
    private final TransactionLogPublisher transactionLogPublisher;
    private final WebhookDispatchPublisher webhookDispatchPublisher;

    @KafkaListener(topics = "${app.kafka.topic.transaction-requests}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TransactionRequestEvent event, Acknowledgment acknowledgement) {
        try {
            ProcessingResult result = transactionProcessingService.process(event);

            if (result.duplicateRejected()) {
                webhookDispatchPublisher.publish(WebhookDispatchEvent.statusUpdate(
                        event.callbackUrl(),
                        event.transactionId(),
                        TransactionStatus.REJECTED,
                        event.correlationId(),
                        event.createdAt()
                ));
                acknowledgement.acknowledge();
                log.info(
                        "Acknowledged duplicate transaction request transactionId={} eventId={}",
                        event.transactionId(),
                        event.eventId()
                );
                return;
            }

            if (result.duplicateReplay()) {
                webhookDispatchPublisher.publish(WebhookDispatchEvent.statusUpdate(
                        event.callbackUrl(),
                        event.transactionId(),
                        TransactionStatus.ACCEPTED,
                        event.correlationId(),
                        event.createdAt()
                ));
                acknowledgement.acknowledge();
                log.info(
                        "Acknowledged replayed transaction request transactionId={} eventId={}",
                        event.transactionId(),
                        event.eventId()
                );
                return;
            }

            transactionLogPublisher.publish(result.transactionLogEvent());
            webhookDispatchPublisher.publish(WebhookDispatchEvent.statusUpdate(
                    result.transactionLogEvent().callbackUrl(),
                    result.transactionLogEvent().transactionId(),
                    TransactionStatus.ACCEPTED,
                    result.transactionLogEvent().correlationId(),
                    result.transactionLogEvent().processedAt()
            ));

            acknowledgement.acknowledge();

            log.info(
                    "Acknowledged processed transaction request transactionId={} eventId={} transactionLogEventId={}",
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
