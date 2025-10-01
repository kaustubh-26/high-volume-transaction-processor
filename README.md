# High Volume Transaction Processor

A fintech-style transaction processing system built with Java, Spring Boot, Kafka, Redis, Postgres, and MongoDB.

## Architecture

```text
Client
  ↓
API Service
  ↓
Kafka topic: transaction_requests
  ↓
Processor Service
  ├─ validation
  ├─ Redis idempotency
  └─ Kafka topic: transaction_log
        ↓
   Ledger Writer Service
   └─ Postgres ledger

   Audit Service
   └─ MongoDB immutable audit events

```

## Scaling model

This system scales horizontally through Kafka partitions and consumer groups.

### Partition strategy
- `transaction_requests` and `transaction_log` use `accountId` as the Kafka message key.
- This preserves event ordering per account within a partition.
- Ordering is guaranteed only within a partition, not across the whole topic.

### Current local setup
- `transaction_requests`: 6 partitions
- `transaction_log`: 6 partitions
- `processor-service` concurrency: 3
- `ledger-writer-service` concurrency: 3
- `audit-service` concurrency: 3

### Important rules
- Useful parallelism is capped by partition count.
- If a topic has 6 partitions, more than 6 consumers in the same group will not increase active consumption.
- Increase partitions carefully because changing partition count can affect key distribution and ordering characteristics for future records.

### Practical scaling path
- Increase topic partitions first when sustained throughput requires more parallelism.
- Then scale service replicas or listener concurrency up to the partition count.
- Keep `accountId` as the partition key to preserve per-account ordering.


## Operational endpoints

Each service exposes:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

## Suggested local checks

- API health: `curl http://localhost:8080/actuator/health`
- Processor health: `curl http://localhost:8081/actuator/health`
- Ledger writer health: `curl http://localhost:8082/actuator/health`
- Audit service health: `curl http://localhost:8083/actuator/health`

## Dead-letter topics

- `transaction_requests_dlt`
- `transaction_log_dlt`

Messages land here after retries are exhausted or when non-retryable errors occur.

## Useful dashboards to add later

- Kafka consumer lag by group
- Ledger batch size
- Redis idempotency hit ratio
- Postgres insert latency
- DLT message rate
- Reconciliation drift count
