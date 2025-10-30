package com.kaustubh.transactions.ledger.webhook;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

public class LedgerWebhookRecoverer implements ConsumerRecordRecoverer {

    private final DeadLetterPublishingRecoverer delegate;
    private final TransactionWebhookNotifier webhookNotifier;

    public LedgerWebhookRecoverer(
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

        if (consumerRecord.value() instanceof TransactionLogEvent event) {
            webhookNotifier.sendStatusUpdate(
                    event.callbackUrl(),
                    TransactionStatus.FAILED,
                    event.transactionId(),
                    event.correlationId(),
                    Instant.now()
            );
        }

        delegate.accept(consumerRecord, ex);
    }
}
