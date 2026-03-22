# Kafka Topics And Event Contracts

Partition counts in this document reflect the local Docker setup in [`infra/kafka/create-topics.sh`](/home/kaustubh/Desktop/projects/Java/high-volume-transaction-processor/infra/kafka/create-topics.sh).

## Topic Inventory

| Topic | Partitions | Producer | Consumers | Key | Purpose |
| --- | --- | --- | --- | --- | --- |
| `transaction_requests` | `6` | `api-service` | `processor-service` (`processor-group`) | `accountId` | Signed inbound transaction requests |
| `transaction_log` | `6` | `processor-service` | `ledger-writer-service` (`ledger-writer-group`), `audit-service` (`audit-group`) | `accountId` | Accepted transaction events for persistence and audit |
| `transaction_requests_dlt` | `6` | Processor error handler | No in-repo consumer | original partition | Failed request records after retries or non-retryable errors |
| `transaction_log_dlt` | `6` | Ledger error handler | No in-repo consumer | original partition | Failed log records after retries or non-retryable errors |

## Event Shapes

### `transaction_requests`

Payload type: `TransactionRequestEvent`

Important fields:

- `eventId`
- `transactionId`
- `idempotencyKey`
- `merchantId`
- `accountId`
- `amount`
- `currency`
- `type`
- `callbackUrl`
- `correlationId`
- `createdAt`

Produced when:

- `api-service` accepts a signed request and returns HTTP `202`

### `transaction_log`

Payload type: `TransactionLogEvent`

Important fields:

- `eventId`
- `transactionId`
- `idempotencyKey`
- `merchantId`
- `accountId`
- `amount`
- `currency`
- `type`
- `status`
- `callbackUrl`
- `correlationId`
- `processedAt`

Produced when:

- `processor-service` validates a request and accepts it for downstream persistence

In the current implementation, the processor emits `transaction_log` records with `status=ACCEPTED`.

## Consumer Behavior

### `processor-service`

- Consumes single `TransactionRequestEvent` records
- Uses manual acknowledgment
- Default listener concurrency: `3`
- Enforces idempotency in Redis before publishing to `transaction_log`

### `ledger-writer-service`

- Consumes `TransactionLogEvent` in batches
- Uses manual acknowledgment
- Default listener concurrency: `3`
- Writes PostgreSQL ledger rows in JDBC batches

### `audit-service`

- Consumes single `TransactionLogEvent` records
- Uses manual acknowledgment
- Default listener concurrency: `3`
- Writes immutable MongoDB audit documents

## Ordering Guarantees

- Ordering is guaranteed only within a partition.
- `accountId` is always used as the Kafka key for the main topics.
- All events for the same account therefore stay ordered relative to each other as long as they land on the same partition.
- There is no global ordering guarantee across accounts.

## Retry And Dead-Letter Policy

### `processor-service`

- Error handler: Spring Kafka `DefaultErrorHandler`
- Backoff: `1s`
- Retry attempts: `2`
- Non-retryable exception: `IllegalArgumentException`
- DLT destination: `<original-topic>_dlt`

Webhook side effects on terminal failure:

- `REJECTED` for non-retryable processor validation/idempotency failures
- `FAILED` for retryable failures that exhaust retries

### `ledger-writer-service`

- Error handler: Spring Kafka `DefaultErrorHandler`
- Backoff: `1s`
- Retry attempts: `2`
- Non-retryable exception: `IllegalArgumentException`
- DLT destination: `<original-topic>_dlt`

Webhook side effect on terminal failure:

- `FAILED`

## Operational Notes

- The repository creates topics automatically in Docker Compose through `kafka-init`.
- Topic partition counts are a throughput and ordering decision, not just a Kafka detail. If you change them, review:
  - consumer concurrency
  - ordering expectations
  - load-test assumptions
- `ledger-writer-service` also contains a replay mode that re-consumes `transaction_log` from the beginning using a dedicated consumer group.
