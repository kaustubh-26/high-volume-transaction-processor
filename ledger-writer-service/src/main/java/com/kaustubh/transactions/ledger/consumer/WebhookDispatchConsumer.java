package com.kaustubh.transactions.ledger.consumer;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.ledger.service.WebhookDeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatchConsumer {

    private final WebhookDeliveryService webhookDeliveryService;

    @KafkaListener(
            topics = "${app.kafka.topic.webhook-dispatch}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "webhookKafkaListenerContainerFactory"
    )
    public void consume(List<WebhookDispatchEvent> events, Acknowledgment acknowledgment) {
        if (events == null || events.isEmpty()) {
            return;
        }

        try {
            webhookDeliveryService.deliverBatch(events);
            acknowledgment.acknowledge();

            log.info(
                    "Acknowledged webhook dispatch batch size={} callbackUrlCount={}",
                    events.size(),
                    events.stream()
                            .map(WebhookDispatchEvent::callbackUrl)
                            .filter(callbackUrl -> callbackUrl != null && !callbackUrl.isBlank())
                            .distinct()
                            .count()
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to deliver webhook dispatch batch size={}",
                    events.size(),
                    ex
            );
            throw ex;
        }
    }
}
