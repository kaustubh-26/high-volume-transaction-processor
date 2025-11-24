package com.kaustubh.transactions.ledger.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.common.kafka.AbstractWebhookDispatchPublisher;
import com.kaustubh.transactions.ledger.config.KafkaTopicProperties;

@Service
public class WebhookDispatchPublisher extends AbstractWebhookDispatchPublisher {

    public WebhookDispatchPublisher(
            KafkaTemplate<String, WebhookDispatchEvent> kafkaTemplate,
            KafkaTopicProperties kafkaTopicProperties
    ) {
        super(kafkaTemplate, kafkaTopicProperties.webhookDispatch());
    }
}
