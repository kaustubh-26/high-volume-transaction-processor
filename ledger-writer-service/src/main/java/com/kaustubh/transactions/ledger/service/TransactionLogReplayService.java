package com.kaustubh.transactions.ledger.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.ledger.config.KafkaTopicProperties;
import com.kaustubh.transactions.ledger.config.ReplayProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionLogReplayService {

    private final LedgerPersistenceService ledgerPersistenceService;
    private final ReplayProperties replayProperties;
    private final KafkaTopicProperties kafkaTopicProperties;
    private final Map<String, Object> replayConsumerProperties;

    protected KafkaConsumer<String, TransactionLogEvent> createConsumer() {
        return new KafkaConsumer<>(replayConsumerProperties);
    }

    public void replayFromBeginning() {
        try (KafkaConsumer<String, TransactionLogEvent> consumer = createConsumer()) {

            List<TopicPartition> partitions = consumer.partitionsFor(kafkaTopicProperties.transactionLog())
                    .stream()
                    .map(this::toTopicPartition)
                    .toList();

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            consumer.poll(Duration.ofMillis(replayProperties.pollTimeoutMs()));

            long totalReplayed = 0;
            long idlePolls = 0;

            while (idlePolls < replayProperties.idleStopAfterPolls()) {
                ConsumerRecords<String, TransactionLogEvent> records = consumer
                        .poll(Duration.ofMillis(replayProperties.pollTimeoutMs()));

                if (records.isEmpty()) {
                    idlePolls++;
                    continue;
                }

                idlePolls = 0;

                List<TransactionLogEvent> events = new java.util.ArrayList<>();

                for (ConsumerRecord<String, TransactionLogEvent> consumerRecord : records
                        .records(kafkaTopicProperties.transactionLog())) {
                    events.add(consumerRecord.value());
                }

                ledgerPersistenceService.persistBatch(events);
                totalReplayed += events.size();

                log.info(
                        "Replay batch persisted batchSize={} totalReplayed={}",
                        events.size(),
                        totalReplayed);
            }

            log.info("Replay completed totalReplayed={}", totalReplayed);
        }
    }

    private TopicPartition toTopicPartition(PartitionInfo partitionInfo) {
        return new TopicPartition(partitionInfo.topic(), partitionInfo.partition());
    }
}
