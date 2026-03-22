# Testing

The repository uses a mix of focused unit tests and infrastructure-backed integration tests.

## Test Stack

- JUnit 5
- AssertJ
- Mockito
- Spring Boot Test
- Testcontainers
- Awaitility
- JaCoCo

## Commands

From the repository root:

```bash
./mvnw test
./mvnw verify
```

What they do:

- `test` runs the module test suites
- `verify` also generates the aggregate JaCoCo coverage report used by the root build

Coverage output:

- HTML: `target/site/jacoco-aggregate/index.html`
- XML: `target/site/jacoco-aggregate/jacoco.xml`

## How The Suite Is Structured

### `api-service`

Mainly unit and slice-style tests for:

- controllers
- exception handling
- correlation ID behavior
- signature verification helpers and filters
- read-auth config parsing and rate-limit filter behavior
- transaction status repository/service logic
- Kafka event publication and ingress service behavior

### `processor-service`

Unit coverage includes:

- request validation logic
- Redis idempotency adapter behavior
- health indicator behavior
- webhook recoverer behavior

Integration coverage includes:

- Kafka + Redis flow for `TransactionRequestConsumer`
- real Redis behavior for `RedisIdempotencyStore`

### `ledger-writer-service`

Unit coverage includes:

- ledger persistence service behavior
- reconciliation job/service logic
- replay service behavior
- webhook recoverer behavior

Integration coverage includes:

- Kafka + PostgreSQL flow for `TransactionLogConsumer`

### `audit-service`

Unit coverage includes:

- audit persistence logic
- audit consumer behavior

Integration coverage includes:

- Kafka + MongoDB flow for `TransactionAuditConsumer`

## Integration Test Characteristics

The integration tests use real infrastructure through Testcontainers rather than mocking Kafka or the backing data stores.

Representative coverage currently includes:

- Kafka -> processor -> `transaction_log`
- Redis-backed idempotency acquisition
- Kafka -> ledger persistence in PostgreSQL
- Kafka -> audit persistence in MongoDB

Important practical note:

- Docker is required for the Testcontainers-backed tests
- if Docker is unavailable, some integration tests will be skipped while others may fail early depending on the module and environment
