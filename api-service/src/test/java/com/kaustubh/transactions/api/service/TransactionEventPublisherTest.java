package com.kaustubh.transactions.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.kaustubh.transactions.api.config.KafkaTopicProperties;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;

@ExtendWith(MockitoExtension.class)
class TransactionEventPublisherTest {

    @Mock
    private KafkaTemplate<String, TransactionRequestEvent> kafkaTemplate;

    @Test
    void publish_sendsToTopicUsingAccountIdKey() {
        KafkaTopicProperties props = new KafkaTopicProperties("transaction-requests");
        TransactionEventPublisher publisher = new TransactionEventPublisher(kafkaTemplate, props);

        TransactionRequestEvent event = new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "acct-9",
                new BigDecimal("12.34"),
                "USD",
                TransactionType.DEBIT,
                "corr-1",
                Instant.now()
        );

        SendResult<String, TransactionRequestEvent> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send("transaction-requests", "acct-9", event))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publish(event);

        verify(kafkaTemplate).send("transaction-requests", "acct-9", event);
    }

    @Test
    void publish_throwsWhenEventIsNull() {
        KafkaTopicProperties props = new KafkaTopicProperties("transaction-requests");
        TransactionEventPublisher publisher = new TransactionEventPublisher(kafkaTemplate, props);

        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionRequestEvent must not be null");
    }

    @Test
    void publish_throwsWhenTopicIsNull() {
        KafkaTopicProperties props = new KafkaTopicProperties(null);
        TransactionEventPublisher publisher = new TransactionEventPublisher(kafkaTemplate, props);

        TransactionRequestEvent event = new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-2",
                "idem-2",
                "acct-2",
                new BigDecimal("1.00"),
                "USD",
                TransactionType.CREDIT,
                "corr-2",
                Instant.now()
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("app.kafka.topic.transaction-requests must not be null");
    }

    @Test
    void publish_throwsWhenAccountIdIsNull() {
        KafkaTopicProperties props = new KafkaTopicProperties("transaction-requests");
        TransactionEventPublisher publisher = new TransactionEventPublisher(kafkaTemplate, props);

        TransactionRequestEvent event = new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-3",
                "idem-3",
                null,
                new BigDecimal("1.00"),
                "USD",
                TransactionType.REFUND,
                "corr-3",
                Instant.now()
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accountId must not be null");
    }
}
