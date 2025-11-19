package com.kaustubh.transactions.common.kafka;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;

public abstract class AbstractWebhookDispatchPublisher {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final KafkaTemplate<String, WebhookDispatchEvent> kafkaTemplate;
    private final String topic;

    protected AbstractWebhookDispatchPublisher(
            KafkaTemplate<String, WebhookDispatchEvent> kafkaTemplate,
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(WebhookDispatchEvent event) {
        if (event == null || event.callbackUrl() == null || event.callbackUrl().isBlank()) {
            return;
        }

        try {
            var result = kafkaTemplate.send(topic, event.callbackUrl(), event).get();

            if (log.isInfoEnabled()) {
                var metadata = result.getRecordMetadata();

                log.info(
                        "Published webhook dispatch event eventId={} transactionId={} status={} topic={} partition={} offset={}",
                        event.eventId(),
                        event.transactionId(),
                        event.status(),
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                );
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing webhook dispatch event", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Failed to publish webhook dispatch event", ex);
        }
    }
}
