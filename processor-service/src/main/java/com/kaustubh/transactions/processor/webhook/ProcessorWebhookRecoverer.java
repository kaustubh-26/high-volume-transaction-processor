package com.kaustubh.transactions.processor.webhook;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.processor.service.WebhookDispatchPublisher;

public class ProcessorWebhookRecoverer implements ConsumerRecordRecoverer {

    private final DeadLetterPublishingRecoverer delegate;
    private final WebhookDispatchPublisher webhookDispatchPublisher;

    public ProcessorWebhookRecoverer(
            DeadLetterPublishingRecoverer delegate,
            WebhookDispatchPublisher webhookDispatchPublisher
    ) {
        this.delegate = delegate;
        this.webhookDispatchPublisher = webhookDispatchPublisher;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> consumerRecord, Exception ex) {
        if (consumerRecord == null) {
            return;
        }
        if (consumerRecord.value() instanceof TransactionRequestEvent event) {
            TransactionStatus status = ex instanceof IllegalArgumentException
                    ? TransactionStatus.REJECTED
                    : TransactionStatus.FAILED;

            webhookDispatchPublisher.publish(WebhookDispatchEvent.statusUpdate(
                    event.callbackUrl(),
                    event.transactionId(),
                    status,
                    event.correlationId(),
                    Instant.now()
            ));
        }

        delegate.accept(consumerRecord, ex);
    }
}
