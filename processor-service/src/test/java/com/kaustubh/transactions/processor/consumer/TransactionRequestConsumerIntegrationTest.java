package com.kaustubh.transactions.processor.consumer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;
import com.kaustubh.transactions.common.event.TransactionRequestEvent;
import com.kaustubh.transactions.common.event.WebhookDispatchEvent;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class TransactionRequestConsumerIntegrationTest {

    static {
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        System.setProperty("jna.tmpdir", tmpDir);
        System.setProperty("org.testcontainers.tmpdir", tmpDir);
    }

    private static final String REQUESTS_TOPIC = "transaction_requests_it";
    private static final String LOG_TOPIC = "transaction_log_it";
    private static final String WEBHOOK_DISPATCH_TOPIC = "webhook_dispatch_it";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "processor-it-" + UUID.randomUUID());
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("app.kafka.topic.transaction-requests", () -> REQUESTS_TOPIC);
        registry.add("app.kafka.topic.transaction-log", () -> LOG_TOPIC);
        registry.add("app.kafka.topic.webhook-dispatch", () -> WEBHOOK_DISPATCH_TOPIC);
        registry.add("app.kafka.listener.concurrency", () -> 1);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    @Qualifier("genericKafkaTemplate")
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void consume_processesRequestAndPublishesTransactionLogEvent() throws Exception {
        createTopic(REQUESTS_TOPIC);
        createTopic(LOG_TOPIC);
        createTopic(WEBHOOK_DISPATCH_TOPIC);

        TransactionRequestEvent event = new TransactionRequestEvent(
                UUID.randomUUID(),
                "tx-1",
                "idem-1",
                "merchant-1",
                "acct-1",
                new BigDecimal("10.00"),
                "USD",
                TransactionType.DEBIT,
                "https://merchant.example/webhook",
                "corr-1",
                Instant.now());

        kafkaTemplate.send(REQUESTS_TOPIC, event.accountId(), event)
                .get(10, TimeUnit.SECONDS);

        try (KafkaConsumer<String, TransactionLogEvent> consumer = new KafkaConsumer<>(createLogConsumerProperties())) {
            consumer.subscribe(List.of(LOG_TOPIC));
            TransactionLogEvent logEvent = pollForLogEvent(consumer, Duration.ofSeconds(15));

            if (logEvent == null) {
                fail("Timed out waiting for transaction log event");
            }

            assertThat(logEvent.transactionId()).isEqualTo(event.transactionId());
            assertThat(logEvent.accountId()).isEqualTo(event.accountId());
            assertThat(logEvent.status()).isEqualTo(TransactionStatus.ACCEPTED);
        }

        try (KafkaConsumer<String, WebhookDispatchEvent> consumer = new KafkaConsumer<>(createWebhookConsumerProperties())) {
            consumer.subscribe(List.of(WEBHOOK_DISPATCH_TOPIC));
            WebhookDispatchEvent dispatchEvent = pollForWebhookDispatchEvent(consumer, Duration.ofSeconds(15));

            if (dispatchEvent == null) {
                fail("Timed out waiting for webhook dispatch event");
            }

            assertThat(dispatchEvent.transactionId()).isEqualTo(event.transactionId());
            assertThat(dispatchEvent.callbackUrl()).isEqualTo(event.callbackUrl());
            assertThat(dispatchEvent.status()).isEqualTo(TransactionStatus.ACCEPTED.name());
        }
    }

    private Properties createLogConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "processor-log-verifier-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kaustubh.transactions.common.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransactionLogEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return props;
    }

    private Properties createWebhookConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "processor-webhook-verifier-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kaustubh.transactions.common.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, WebhookDispatchEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return props;
    }

    private TransactionLogEvent pollForLogEvent(
            KafkaConsumer<String, TransactionLogEvent> consumer,
            Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, TransactionLogEvent> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records.iterator().next().value();
            }
        }
        return null;
    }

    private WebhookDispatchEvent pollForWebhookDispatchEvent(
            KafkaConsumer<String, WebhookDispatchEvent> consumer,
            Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, WebhookDispatchEvent> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records.iterator().next().value();
            }
        }
        return null;
    }

    private void createTopic(String topic)
            throws InterruptedException, ExecutionException, TimeoutException {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(props)) {
            try {
                adminClient.createTopics(List.of(new NewTopic(topic, 1, (short) 1)))
                        .all()
                        .get(10, TimeUnit.SECONDS);
            } catch (ExecutionException ex) {
                if (!(ex.getCause() instanceof TopicExistsException)) {
                    throw ex;
                }
            }
        }
    }
}
