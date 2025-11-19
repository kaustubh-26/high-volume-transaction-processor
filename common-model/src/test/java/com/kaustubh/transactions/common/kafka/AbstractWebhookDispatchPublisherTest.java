package com.kaustubh.transactions.common.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;

@ExtendWith(MockitoExtension.class)
class AbstractWebhookDispatchPublisherTest {

    @Mock
    private KafkaTemplate<String, WebhookDispatchEvent> kafkaTemplate;

    private TestWebhookDispatchPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TestWebhookDispatchPublisher(kafkaTemplate, "webhook_dispatch");
    }

    @Test
    void publish_skipsNullEvent() {
        publisher.publish(null);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_skipsBlankCallbackUrl() {
        WebhookDispatchEvent event = new WebhookDispatchEvent(
                UUID.randomUUID(),
                "   ",
                "tx-1",
                "FAILED",
                "corr-1",
                java.time.Instant.now()
        );

        publisher.publish(event);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_sendsEventWhenValid() throws Exception {
        WebhookDispatchEvent event = new WebhookDispatchEvent(
                UUID.randomUUID(),
                "https://merchant.example/webhook",
                "tx-2",
                "PERSISTED",
                "corr-2",
                java.time.Instant.parse("2026-03-22T10:10:00Z")
        );

        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, WebhookDispatchEvent>> sendFuture = mock(CompletableFuture.class);
        SendResult<String, WebhookDispatchEvent> sendResult = mock(SendResult.class);
        RecordMetadata recordMetadata = mock(RecordMetadata.class);

        when(kafkaTemplate.send("webhook_dispatch", event.callbackUrl(), event)).thenReturn(sendFuture);
        when(sendFuture.get()).thenReturn(sendResult);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.topic()).thenReturn("webhook_dispatch");
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(123L);

        publisher.publish(event);

        verify(kafkaTemplate).send("webhook_dispatch", event.callbackUrl(), event);
        verify(sendFuture).get();
    }

    @Test
    void publish_wrapsExecutionFailures() throws Exception {
        WebhookDispatchEvent event = new WebhookDispatchEvent(
                UUID.randomUUID(),
                "https://merchant.example/webhook",
                "tx-3",
                "FAILED",
                "corr-3",
                java.time.Instant.parse("2026-03-22T10:12:00Z")
        );

        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, WebhookDispatchEvent>> sendFuture = mock(CompletableFuture.class);
        ExecutionException failure = new ExecutionException("boom", new RuntimeException("broker-down"));

        when(kafkaTemplate.send("webhook_dispatch", event.callbackUrl(), event)).thenReturn(sendFuture);
        when(sendFuture.get()).thenThrow(failure);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to publish webhook dispatch event")
                .hasCause(failure);
    }

    private static class TestWebhookDispatchPublisher extends AbstractWebhookDispatchPublisher {

        TestWebhookDispatchPublisher(KafkaTemplate<String, WebhookDispatchEvent> kafkaTemplate, String topic) {
            super(kafkaTemplate, topic);
        }
    }
}
