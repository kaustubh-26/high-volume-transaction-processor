# Architecture Overview

This repository implements an asynchronous transaction pipeline with a separate write path, audit path, and read path.

## Service Responsibilities

| Component | Responsibility | Key Dependencies |
| --- | --- | --- |
| `api-service` | Accepts signed transaction requests, publishes `TransactionRequestEvent`, exposes the transaction status endpoint | Kafka, Postgres, Redis |
| `processor-service` | Validates requests, enforces idempotency, emits `TransactionLogEvent`, sends webhook updates for accepted/rejected/failed processing outcomes | Kafka, Redis |
| `ledger-writer-service` | Batch-writes ledger rows, sends persisted/failed webhooks, runs reconciliation, optionally replays `transaction_log` from the beginning | Kafka, Postgres, MongoDB |
| `audit-service` | Stores immutable audit documents for every accepted log event | Kafka, MongoDB |
| `common-model` | Shared request DTOs, event contracts, enums, and webhook payloads | N/A |

## Primary Write Path

1. A client sends `POST /api/v1/transactions` to `api-service`.
2. `VerifySignatureFilter` checks request headers, timestamp skew, and request signature.
3. `api-service` generates a transaction ID and publishes `TransactionRequestEvent` to `transaction_requests` with `accountId` as the Kafka key.
4. `processor-service` consumes the request, validates it, and uses Redis to acquire an idempotency key.
5. If the request is new, the processor publishes `TransactionLogEvent` to `transaction_log` with status `ACCEPTED`.
6. `ledger-writer-service` consumes `transaction_log` in batches and inserts rows into PostgreSQL `ledger_entries` with status `PERSISTED`.
7. `audit-service` independently consumes `transaction_log` and stores an immutable document in MongoDB `transaction_audit_events`.

## Read Path

The read path is intentionally simpler than the write path:

1. A client sends `GET /api/v1/transactions/{transactionId}/status` with `X-API-Key`.
2. `ReadApiKeyRateLimitFilter` validates the API key and applies Redis-backed rate limiting.
3. `api-service` checks whether the API key is allowed to read transactions for the merchant that owns the row.
4. `api-service` queries PostgreSQL `ledger_entries` directly and returns the stored status.

> [!IMPORTANT]
> The current status endpoint is a ledger-backed view, not a full workflow state machine. In the current flow, a transaction usually appears there only after the ledger writer inserts it, so successful local requests typically resolve to `PERSISTED` and anything not yet persisted resolves to `404`.

## Transaction Lifecycle

| Status | Where It Appears | Meaning |
| --- | --- | --- |
| `RECEIVED` | `api-service` `202` response | Request accepted at ingress and published to Kafka |
| `ACCEPTED` | `transaction_log`, processor webhook | Request validated and accepted for downstream persistence |
| `PERSISTED` | PostgreSQL `ledger_entries`, ledger webhook, status endpoint | Ledger write completed successfully |
| `REJECTED` | Processor webhook, request DLT handling for validation/idempotency failures | Request was invalid or duplicate |
| `FAILED` | Processor/ledger webhook and DLT handling | Processing failed after retries |

## Ordering And Idempotency

- Kafka ordering is guaranteed per partition, not globally.
- The project uses `accountId` as the Kafka message key so all events for an account stay on the same partition.
- `processor-service` stores idempotency keys in Redis with a TTL and rejects duplicates before they reach the ledger topic.
- PostgreSQL also enforces uniqueness through:
  - `transaction_id`
  - `(account_id, idempotency_key)`

## Failure Handling

- `processor-service` and `ledger-writer-service` both use Spring Kafka `DefaultErrorHandler`.
- Both handlers retry with a `1s` backoff and `2` retry attempts before dead-lettering.
- Failed request records are published to `transaction_requests_dlt`.
- Failed log records are published to `transaction_log_dlt`.
- Webhook recoverers send:
  - `REJECTED` for non-retryable processor validation/idempotency failures
  - `FAILED` for processor or ledger failures that exhaust retries

## Webhooks

If the client provides `callbackUrl`, the platform sends:

- `ACCEPTED` from `processor-service`
- `PERSISTED` from `ledger-writer-service`
- `REJECTED` or `FAILED` from the processor/ledger recoverers when appropriate

Webhook delivery is best-effort. A callback failure is logged but does not roll back Kafka acknowledgment or database writes.

## Reconciliation And Replay

`ledger-writer-service` also owns two operational features:

- Reconciliation:
  - Compares recent audit events in MongoDB with recent ledger rows in PostgreSQL
  - Writes summary rows into `reconciliation_runs`
- Replay:
  - When enabled, consumes `transaction_log` from the beginning using a dedicated consumer group
  - Rebuilds ledger state by reusing the same batch persistence path

The service depends on MongoDB in Docker Compose because reconciliation reads audit data directly.

## Scaling Notes

- The local topic creation script configures `6` partitions for the main and dead-letter topics.
- Default consumer concurrency is `3`, so local settings stay below the partition count.
- Scaling useful parallelism generally means increasing partition counts first, then consumer concurrency.
- The API is synchronous only at ingress. Downstream processing remains asynchronous, so HTTP `202` throughput is not the same as end-to-end persistence throughput.
