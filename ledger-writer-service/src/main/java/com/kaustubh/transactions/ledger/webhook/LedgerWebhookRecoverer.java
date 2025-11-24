package com.kaustubh.transactions.ledger.webhook;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.service.WebhookDispatchPublisher;

public class LedgerWebhookRecoverer implements ConsumerRecordRecoverer {

    private final DeadLetterPublishingRecoverer delegate;
    private final WebhookDispatchPublisher webhookDispatchPublisher;

    public LedgerWebhookRecoverer(
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

        if (consumerRecord.value() instanceof TransactionLogEvent event) {
            webhookDispatchPublisher.publish(WebhookDispatchEvent.statusUpdate(
                    event.callbackUrl(),
                    event.transactionId(),
                    TransactionStatus.FAILED,
                    event.correlationId(),
                    Instant.now()
            ));
        }

        delegate.accept(consumerRecord, ex);
    }
}
