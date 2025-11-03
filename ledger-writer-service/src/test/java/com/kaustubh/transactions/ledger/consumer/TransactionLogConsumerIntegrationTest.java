package com.kaustubh.transactions.ledger.consumer;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.kaustubh.transactions.common.enums.TransactionStatus;
import com.kaustubh.transactions.common.enums.TransactionType;
import com.kaustubh.transactions.common.event.TransactionLogEvent;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TransactionLogConsumerIntegrationTest {

    static {
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        System.setProperty("jna.tmpdir", tmpDir);
        System.setProperty("org.testcontainers.tmpdir", tmpDir);
    }

    private static final String LOG_TOPIC = "transaction_log_it";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("payments")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "ledger-it-" + UUID.randomUUID());
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("app.kafka.topic.transaction-log", () -> LOG_TOPIC);
        registry.add("app.kafka.listener.concurrency", () -> 1);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.reconciliation.enabled", () -> false);
        registry.add("app.replay.enabled-on-startup", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ledger_entries (
                    id BIGSERIAL PRIMARY KEY,
                    transaction_id VARCHAR(64) NOT NULL,
                    idempotency_key VARCHAR(128) NOT NULL,
                    account_id VARCHAR(64) NOT NULL,
                    amount NUMERIC(18,2) NOT NULL,
                    currency VARCHAR(8) NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    processed_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_ledger_transaction_id
                    ON ledger_entries (transaction_id)
                """);
    }

    @Test
    void consume_persistsBatchToLedger() throws Exception {
        createTopic(LOG_TOPIC);

        String transactionId = "tx-" + UUID.randomUUID();
        TransactionLogEvent event = new TransactionLogEvent(
                UUID.randomUUID(),
                transactionId,
                "idem-1",
                "acct-1",
                new BigDecimal("12.34"),
                "USD",
                TransactionType.CREDIT,
                TransactionStatus.ACCEPTED,
                null,
                "corr-1",
                Instant.now()
        );

        sendEvent(event);

        Integer count = pollForInsert(transactionId, Duration.ofSeconds(15));
        if (count == null || count == 0) {
            fail("Timed out waiting for ledger entry insert");
        }

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM ledger_entries WHERE transaction_id = ?",
                String.class,
                transactionId
        );
        assertThat(status).isEqualTo(TransactionStatus.PERSISTED.name());
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

    private Integer pollForInsert(String transactionId, Duration timeout) {
        return await()
                .atMost(timeout)
                .pollInterval(Duration.ofMillis(200))
                .until(() -> jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM ledger_entries WHERE transaction_id = ?",
                            Integer.class,
                            transactionId
                        ),
                    count -> count != null && count > 0);
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
