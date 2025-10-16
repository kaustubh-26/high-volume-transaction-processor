package com.kaustubh.transactions.audit.consumer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.kaustubh.transactions.audit.document.TransactionAuditEventDocument;
import com.kaustubh.transactions.audit.repository.TransactionAuditEventRepository;
import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TransactionAuditConsumerIntegrationTest {

    static {
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        System.setProperty("jna.tmpdir", tmpDir);
        System.setProperty("org.testcontainers.tmpdir", tmpDir);
    }

    private static final String LOG_TOPIC = "transaction_log_it";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Container
    static final MongoDBContainer MONGODB = new MongoDBContainer(
            DockerImageName.parse("mongo:6.0"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "audit-it-" + UUID.randomUUID());
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("app.kafka.topic.transaction-log", () -> LOG_TOPIC);
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
    }

    @Autowired
    private TransactionAuditEventRepository repository;

    @BeforeEach
    void clearRepository() {
        repository.deleteAll();
    }

    @Test
    void consume_persistsAuditEvent() throws Exception {
        createTopic(LOG_TOPIC);

        String transactionId = "tx-" + UUID.randomUUID();
        TransactionLogEvent event = new TransactionLogEvent(
                UUID.randomUUID(),
                transactionId,
                "idem-1",
                "acct-1",
                new BigDecimal("25.00"),
                "USD",
                TransactionType.DEBIT,
                TransactionStatus.ACCEPTED,
                "corr-1",
                Instant.now()
        );

        sendEvent(event);

        TransactionAuditEventDocument document = pollForAuditEvent(transactionId, Duration.ofSeconds(15));
        if (document == null) {
            fail("Timed out waiting for audit event persistence");
        }

        assertThat(document.transactionId()).isEqualTo(transactionId);
        assertThat(document.status()).isEqualTo(TransactionStatus.ACCEPTED.name());
        assertThat(document.sourceTopic()).isEqualTo(LOG_TOPIC);
    }

    private void sendEvent(TransactionLogEvent event)
            throws InterruptedException, ExecutionException, TimeoutException {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        try (KafkaProducer<String, TransactionLogEvent> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(LOG_TOPIC, event.accountId(), event))
                    .get(10, TimeUnit.SECONDS);
        }
    }

    private TransactionAuditEventDocument pollForAuditEvent(String transactionId, Duration timeout) {
        return await()
                .atMost(timeout)
                .pollInterval(Duration.ofMillis(200))
                .until(() -> repository.findAll().stream()
                        .filter(doc -> transactionId.equals(doc.transactionId()))
                        .findFirst()
                        .orElse(null),
                    Objects::nonNull);
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
