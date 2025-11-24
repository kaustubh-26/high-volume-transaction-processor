package com.kaustubh.transactions.ledger.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.common.webhook.TransactionStatusUpdate;
import com.kaustubh.transactions.common.webhook.TransactionWebhookNotifier;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final TransactionWebhookNotifier webhookNotifier;

    public void deliverBatch(List<WebhookDispatchEvent> events) {
        Map<String, List<TransactionStatusUpdate>> payloadsByCallbackUrl = events.stream()
                .filter(this::hasCallbackUrl)
                .collect(Collectors.groupingBy(
                        WebhookDispatchEvent::callbackUrl,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                WebhookDispatchEvent::toStatusUpdate,
                                Collectors.toList()
                        )
                ));

        payloadsByCallbackUrl.forEach(webhookNotifier::sendStatusUpdates);
    }

    private boolean hasCallbackUrl(WebhookDispatchEvent event) {
        return event != null && event.callbackUrl() != null && !event.callbackUrl().isBlank();
    }
}
