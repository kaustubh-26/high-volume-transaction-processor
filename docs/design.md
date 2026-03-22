# Design Decisions And Trade-offs

This document summarizes the main design choices in the codebase and the trade-offs they imply.

## 1. Event-Driven Write Path

The write path is intentionally asynchronous:

- `api-service` accepts and validates the HTTP request shape
- `processor-service` owns business validation and idempotency
- `ledger-writer-service` owns durable ledger persistence
- `audit-service` owns immutable audit persistence

Why this shape works well here:

- services stay focused on one main responsibility
- Kafka decouples request acceptance from downstream persistence
- ledger and audit consumers can scale independently

Trade-off:

- HTTP `202` only means "accepted for processing"
- the client must use the status endpoint or webhooks for downstream visibility

## 2. Ordering By Account, Not Globally

Kafka messages are keyed by `accountId`, which gives the system per-account ordering guarantees without forcing a single global queue.

Benefits:

- preserves the most important ordering boundary for account-level transaction streams
- lets the system scale horizontally with partitions

Trade-off:

- ordering is not guaranteed across different accounts
- increasing throughput usually requires deliberate partition planning

## 3. Redis Idempotency In The Processor

The processor owns idempotency via `RedisIdempotencyStore`.

Benefits:

- prevents duplicate requests from producing duplicate ledger or audit events
- keeps ingress fast by moving deduplication to the asynchronous processing stage
- TTL-backed keys keep the Redis footprint bounded

Trade-off:

- the API still accepts duplicate HTTP submissions and returns `202`; the duplicate is rejected later in the processor
- duplicate or invalid requests do not currently produce a queryable row in the status endpoint because the status endpoint is backed by `ledger_entries`

## 4. Ledger As The Query Backing Store

`api-service` reads transaction status from PostgreSQL `ledger_entries`.

Benefits:

- simple to reason about
- avoids introducing a separate read model or cache for the current scope
- ties the status endpoint directly to durable ledger persistence

Trade-off:

- the endpoint is currently a persisted-ledger view, not a complete workflow state store
- in-flight, rejected, or failed-before-ledger transactions generally appear as `404`

This is a pragmatic choice for the current project, but it is not a full CQRS read model yet.

## 5. Batched Ledger Writes

`ledger-writer-service` uses JDBC batch inserts with configurable batch size.

Benefits:

- improves throughput compared with one insert per event
- keeps write logic straightforward

Trade-off:

- batch consumers increase operational complexity compared with single-record listeners
- clients only observe the persisted state after the batch completes successfully

## 6. Immutable Audit Trail In MongoDB

The audit service stores every accepted `TransactionLogEvent` as a document in MongoDB.

Benefits:

- append-only history is separate from the ledger write model
- audit storage can evolve independently from the relational schema
- indexed lookup fields support reconciliation and investigations

Trade-off:

- the system becomes eventually consistent across Postgres and MongoDB
- drift detection is required because two durable sinks consume the same Kafka topic independently

## 7. Reconciliation And Replay As Operational Safeguards

The ledger writer includes:

- scheduled reconciliation against MongoDB audit events
- optional replay from the beginning of `transaction_log`

Benefits:

- gives the project a realistic recovery story
- makes drift visible in `reconciliation_runs`
- keeps replay on the same persistence path as normal processing

Trade-off:

- replay and reconciliation add operational surface area
- replay is opt-in and not yet wrapped in dedicated CLI/admin tooling

## 8. Webhook Notifications Are Best-Effort

If the request includes `callbackUrl`, webhook updates are emitted from the processor and ledger writer.

Current webhook states:

- `ACCEPTED`
- `PERSISTED`
- `REJECTED`
- `FAILED`

Benefits:

- merchants can receive state changes without polling
- notifications are emitted close to the component that owns the state transition

Trade-off:

- webhook failures are logged but do not block Kafka acknowledgment or database writes
- there is no built-in retry queue or signature scheme for outbound webhooks yet

## 9. Failure Handling Through Kafka DLTs

The processor and ledger writer both use Spring Kafka error handlers that retry twice with a one-second backoff before publishing to dead-letter topics.

Benefits:

- poison messages do not block the main consumer groups indefinitely
- failure handling is explicit and observable

Trade-off:

- the repository does not yet include replay tooling for DLT topics
- operators still need manual investigation and replay decisions

## 10. Current Limitations And Likely Next Steps

The implementation already contains several good extension points, but these are the main gaps to keep in mind:

- Nonce replay protection in `api-service` is configurable but in-memory by default, not distributed.
- The status endpoint is ledger-backed and does not expose the full transaction lifecycle.
- Outbound webhooks are not authenticated or retried through a durable queue.
- Event schemas are plain JSON objects; there is no schema registry or compatibility guardrail yet.
- Distributed tracing across HTTP, Kafka, and webhooks is not implemented yet.

For a portfolio-grade system, these are reasonable trade-offs. They keep the code understandable while still showing realistic architecture, recovery hooks, and operational concerns.
