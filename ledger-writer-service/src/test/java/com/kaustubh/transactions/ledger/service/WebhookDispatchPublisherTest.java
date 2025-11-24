package com.kaustubh.transactions.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.kaustubh.transactions.common.event.WebhookDispatchEvent;
import com.kaustubh.transactions.ledger.config.KafkaTopicProperties;

@ExtendWith(MockitoExtension.class)
class WebhookDispatchPublisherTest {

    @Mock
    private KafkaTemplate<String, WebhookDispatchEvent> kafkaTemplate;

    private WebhookDispatchPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new WebhookDispatchPublisher(
                kafkaTemplate,
                new KafkaTopicProperties("transaction_log", "webhook_dispatch")
        );
    }

    @Test
    void publish_skipsNullEvent() {
        publisher.publish(null);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_skipsBlankCallbackUrl() {
        WebhookDispatchEvent event = dispatchEvent("tx-1", "   ");

        publisher.publish(event);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_sendsUsingCallbackUrlAsKey() throws Exception {
        WebhookDispatchEvent event = dispatchEvent("tx-1", "https://merchant.example/webhook");
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, WebhookDispatchEvent>> sendFuture = mock(CompletableFuture.class);
        @SuppressWarnings("unchecked")
        SendResult<String, WebhookDispatchEvent> sendResult = mock(SendResult.class);
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("webhook_dispatch", 1),
                2L,
                0,
                Instant.parse("2026-03-22T10:20:00Z").toEpochMilli(),
                0,
                0
        );

        when(kafkaTemplate.send("webhook_dispatch", event.callbackUrl(), event)).thenReturn(sendFuture);
        when(sendFuture.get()).thenReturn(sendResult);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        publisher.publish(event);

        verify(kafkaTemplate).send("webhook_dispatch", event.callbackUrl(), event);
        verify(sendFuture).get();
    }

    @Test
    void publish_restoresInterruptFlagWhenInterrupted() throws Exception {
        WebhookDispatchEvent event = dispatchEvent("tx-2", "https://merchant.example/webhook");
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, WebhookDispatchEvent>> sendFuture = mock(CompletableFuture.class);

        when(kafkaTemplate.send("webhook_dispatch", event.callbackUrl(), event)).thenReturn(sendFuture);
        when(sendFuture.get()).thenThrow(new InterruptedException("boom"));

        try {
            assertThatThrownBy(() -> publisher.publish(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Interrupted while publishing webhook dispatch event")
                    .hasCauseInstanceOf(InterruptedException.class);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void publish_wrapsExecutionFailures() throws Exception {
        WebhookDispatchEvent event = dispatchEvent("tx-3", "https://merchant.example/webhook");
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

    private WebhookDispatchEvent dispatchEvent(String transactionId, String callbackUrl) {
        return new WebhookDispatchEvent(
                UUID.randomUUID(),
                callbackUrl,
                transactionId,
                "FAILED",
                "corr-1",
                Instant.parse("2026-03-22T10:15:30Z")
        );
    }
}
