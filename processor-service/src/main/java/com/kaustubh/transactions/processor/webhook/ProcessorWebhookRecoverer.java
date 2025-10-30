package com.kaustubh.transactions.processor.webhook;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

public class ProcessorWebhookRecoverer implements ConsumerRecordRecoverer {

    private final DeadLetterPublishingRecoverer delegate;
    private final TransactionWebhookNotifier webhookNotifier;

    public ProcessorWebhookRecoverer(
            DeadLetterPublishingRecoverer delegate,
            TransactionWebhookNotifier webhookNotifier
    ) {
        this.delegate = delegate;
        this.webhookNotifier = webhookNotifier;
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

            webhookNotifier.sendStatusUpdate(
                    event.callbackUrl(),
                    status,
                    event.transactionId(),
                    event.correlationId(),
                    Instant.now()
            );
        }

        delegate.accept(consumerRecord, ex);
    }
}
