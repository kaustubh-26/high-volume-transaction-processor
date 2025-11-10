package com.kaustubh.transactions.ledger.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.config.ReplayProperties;

@Tag("unit")
class TransactionLogReplayServiceTest {

    @Test
    void replayFromBeginning_processesRecords_andPersistsBatch() {
        LedgerPersistenceService persistenceService = mock(LedgerPersistenceService.class);

        ReplayProperties props = new ReplayProperties(
                false,
                "test-replay-group",
                100,
                10L,
                1L
        );

        KafkaConsumer<String, TransactionLogEvent> consumer = mock(KafkaConsumer.class);

        TransactionLogReplayService service = spy(
                new TransactionLogReplayService(persistenceService, props, Map.of())
        );

        doReturn(consumer).when(service).createConsumer();

        ReflectionTestUtils.setField(service, "transactionLogTopic", "topic");

        PartitionInfo partitionInfo = new PartitionInfo("topic", 0, null, null, null);
        when(consumer.partitionsFor("topic")).thenReturn(List.of(partitionInfo));

        TransactionLogEvent event = new TransactionLogEvent(
                UUID.randomUUID(),
                "tx-123",
                "idem-123",
                "merchant-1",
                "acc-123",
                new BigDecimal("250.00"),
                "INR",
                TransactionType.CREDIT,
                TransactionStatus.ACCEPTED,
                null,
                "corr-123",
                Instant.now()
        );

        ConsumerRecord<String, TransactionLogEvent> consumerRecord =
                new ConsumerRecord<>("topic", 0, 0L, "key", event);

        ConsumerRecords<String, TransactionLogEvent> records =
                new ConsumerRecords<>(Map.of(
                        new TopicPartition("topic", 0),
                        List.of(consumerRecord)
                ));

        when(consumer.poll(any(Duration.class)))
                .thenReturn(ConsumerRecords.empty())   // initial poll after seekToBeginning()
                .thenReturn(records)                  // first replay loop poll
                .thenReturn(ConsumerRecords.empty()); // stop after one idle poll

        service.replayFromBeginning();

        verify(consumer).assign(any());
        verify(consumer).seekToBeginning(any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TransactionLogEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(persistenceService).persistBatch(captor.capture());

        assertThat(captor.getValue()).containsExactly(event);
    }
}
