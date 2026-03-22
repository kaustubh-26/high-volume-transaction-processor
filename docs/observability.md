# Observability

Every Spring Boot service in this repository exposes Actuator health and Prometheus metrics endpoints.

## Local Endpoints

| Service | Base URL | Health | Prometheus |
| --- | --- | --- | --- |
| `api-service` | `http://localhost:8080` | `/actuator/health` | `/actuator/prometheus` |
| `processor-service` | `http://localhost:8081` | `/actuator/health` | `/actuator/prometheus` |
| `ledger-writer-service` | `http://localhost:8082` | `/actuator/health` | `/actuator/prometheus` |
| `audit-service` | `http://localhost:8083` | `/actuator/health` | `/actuator/prometheus` |

Additional API-only endpoint:

- `GET /ping` on `api-service`

## What Is Enabled

All services expose:

- `health`
- `info`
- `prometheus`

The relevant Spring Boot settings are already enabled in each module `application.yml`.

## Correlation IDs

`api-service` adds and propagates `X-Correlation-Id`:

- if the client sends one, it is reused
- otherwise the API generates one
- the response includes the same header
- the value is carried into Kafka events and webhook payloads

This makes it the easiest field to use when tracing a request across:

- API logs
- Kafka payloads
- ledger and audit writes
- webhook notifications

## Useful Local Checks

### Basic Health

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### Prometheus Scrape Spot Checks

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
curl http://localhost:8083/actuator/prometheus
```

## Operational Signals Worth Watching

During local testing or load runs, the most useful signals are:

- API `202` rate and latency
- processor backlog and Kafka lag
- Redis availability for idempotency and read-path rate limiting
- PostgreSQL write latency and row growth in `ledger_entries`
- MongoDB audit write health
- contents of `transaction_requests_dlt` and `transaction_log_dlt`
- reconciliation output in `reconciliation_runs`

> [!NOTE]
> Kafka lag dashboards, distributed tracing, and alerting rules are not bundled in this repository. The codebase exposes enough health and metrics surfaces to integrate them, but those operational layers are not implemented yet.
