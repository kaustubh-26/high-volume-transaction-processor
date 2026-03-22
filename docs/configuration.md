# Configuration

Local development is driven by environment variables. Docker Compose loads `.env` automatically, while direct `spring-boot:run` usage can rely on shell environment variables or JVM property overrides.

## Required Environment Variables

| Variable | Used By | Default | Notes |
| --- | --- | --- | --- |
| `POSTGRES_PASSWORD` | Postgres container, `api-service`, `ledger-writer-service` | `postgres` in Docker Compose | Required in `.env` for local Docker usage |
| `APP_MERCHANT_DEMO_SALT_KEY` | `api-service` signing config and demo Swagger/k6 flows | `merchant-demo-secret-key` | Secret used in `X-Verify` generation for `merchant-demo` |

Minimal `.env` example:

```env
POSTGRES_PASSWORD=postgres
APP_MERCHANT_DEMO_SALT_KEY=merchant-demo-secret-key
```

## Read API Configuration

The transaction status endpoint is closed by default unless you configure at least one read API key.

### Environment Variables

| Variable | Default | Meaning |
| --- | --- | --- |
| `APP_READ_API_KEYS` | empty | Comma-separated allowed API keys |
| `APP_READ_RATE_LIMITS` | empty | Per-key limits in `key:limit` format, comma-separated |
| `APP_READ_RATE_WINDOW_SECONDS` | `60` | Redis rate-limit window length |
| `APP_READ_DEFAULT_LIMIT_PER_WINDOW` | `60` | Fallback rate limit when a key has no explicit override |
| `APP_READ_API_KEY_MERCHANTS` | empty | Merchant ownership mapping in `key:merchantA\|merchantB` format |
| `APP_READ_CONFIG_REDIS_KEY` | `read-auth:config` | Redis hash key used for runtime config |
| `APP_READ_OWNERSHIP_TTL_HOURS` | `24` | Present in config properties but not currently consumed by the implementation |

Example local values:

```env
APP_READ_API_KEYS=read-key-demo
APP_READ_RATE_LIMITS=read-key-demo:120
APP_READ_API_KEY_MERCHANTS=read-key-demo:merchant-demo
```

### Runtime Overrides In Redis

`ReadAuthConfigService` first checks a Redis hash and falls back to the environment properties if the hash is missing or incomplete.

Default Redis hash key:

```text
read-auth:config
```

Supported hash fields:

- `api_keys`
- `rate_limits`
- `rate_window_seconds`
- `default_limit_per_window`
- `api_key_merchants`

Example:

```bash
docker exec -it hvtp-redis redis-cli HSET read-auth:config \
  api_keys "read-key-demo" \
  rate_limits "read-key-demo:120" \
  rate_window_seconds "60" \
  default_limit_per_window "60" \
  api_key_merchants "read-key-demo:merchant-demo"
```

## Request Signing Settings

`api-service` reads merchant signing settings from `app.security.verify`.

Current local defaults:

| Property | Default |
| --- | --- |
| `allowed-clock-skew` | `2m` |
| `nonce-enforced` | `false` |
| `nonce-ttl` | `2m` |
| `merchant-demo.salt-index` | `1` |
| `merchant-demo.salt-key` | `APP_MERCHANT_DEMO_SALT_KEY` or `merchant-demo-secret-key` |

The request signature format is:

```text
SHA256_HEX_UPPER(BASE64(body) + requestPath + timestamp + nonce + idempotencyKey + saltKey) + "###" + saltIndex
```

## Service Defaults

### Docker Compose Ports

| Service | Host Port | Internal Port |
| --- | --- | --- |
| `api-service` | `8080` | `8080` |
| `processor-service` | `8081` | `8080` |
| `ledger-writer-service` | `8082` | `8080` |
| `audit-service` | `8083` | `8080` |
| Kafka | `9092` | `9092` |
| Redis | `6379` | `6379` |
| Postgres | `5432` | `5432` |
| MongoDB | `27017` | `27017` |

### Storage Defaults

| Store | Default |
| --- | --- |
| Postgres database | `payments` |
| Postgres user | `postgres` |
| MongoDB database | `payment_audit` |
| Kafka bootstrap servers | `localhost:9092` outside Docker, `kafka:29092` inside Docker |

## Tunable Application Properties

These properties are already wired in the services and are useful when tuning local throughput or recovery behavior.

### `processor-service`

| Property | Default | Meaning |
| --- | --- | --- |
| `app.kafka.listener.concurrency` | `3` | Consumer concurrency for `transaction_requests` |
| `app.idempotency.key-prefix` | `txn:idem:` | Redis key prefix for idempotency entries |
| `app.idempotency.ttl-hours` | `48` | TTL for idempotency keys |

### `ledger-writer-service`

| Property | Default | Meaning |
| --- | --- | --- |
| `app.kafka.listener.concurrency` | `3` | Consumer concurrency for `transaction_log` |
| `app.ledger.batch-size` | `100` | JDBC batch size for ledger writes |
| `app.reconciliation.enabled` | `true` | Enables scheduled reconciliation |
| `app.reconciliation.fixed-delay-ms` | `60000` | Delay between reconciliation runs |
| `app.reconciliation.lookback-minutes` | `15` | Window used when comparing audit vs ledger data |
| `app.replay.enabled-on-startup` | `false` | Replays `transaction_log` on startup when enabled |
| `app.replay.consumer-group-id` | `ledger-replay-group` | Dedicated replay consumer group |
| `app.replay.max-poll-records` | `200` | Replay consumer poll size |
| `app.replay.poll-timeout-ms` | `1000` | Replay consumer poll timeout |
| `app.replay.idle-stop-after-polls` | `5` | Replay stops after this many empty polls |

## Spring Overrides For Non-Docker Runs

These are the most common overrides when running modules directly on the host:

- `SERVER_PORT`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATA_MONGODB_URI`

See [Running Locally](running-locally.md) for concrete commands.

## Safety Note

All defaults in this repository are for local development only. Replace demo secrets, passwords, and API keys before using any part of this stack in a shared or production-like environment.
