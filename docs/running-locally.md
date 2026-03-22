# Running Locally

This project is easiest to run with Docker Compose, but you can also run the services directly from Maven if you want a faster inner loop.

## Prerequisites

- Docker and Docker Compose
- JDK 21
- Optional: k6 for load testing

## Option 1: Full Stack With Docker Compose

### 1. Prepare `.env`

Copy `.env.example` to `.env` and set at least:

```env
POSTGRES_PASSWORD=postgres
APP_MERCHANT_DEMO_SALT_KEY=merchant-demo-secret-key
```

If you also want the status endpoint:

```env
APP_READ_API_KEYS=read-key-demo
APP_READ_API_KEY_MERCHANTS=read-key-demo:merchant-demo
APP_READ_RATE_LIMITS=read-key-demo:120
```

### 2. Start The Stack

```bash
docker compose up --build
```

### 3. Verify It

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

Helpful local URLs:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`
- `http://localhost:8080/ping`

### 4. Exercise The API

Recommended local options:

- Swagger UI for quick manual requests
- k6 scripts for repeatable signed traffic

The bundled Swagger UI auto-generates the demo signing headers used by `merchant-demo`.

### 5. Stop It

```bash
docker compose down
```

If you also want to remove local data volumes:

```bash
docker compose down -v
```

## Option 2: Run Services Outside Docker

Run only the infrastructure with Docker first:

```bash
docker compose up -d kafka kafka-init redis postgres mongo
```

Then start each service in a separate terminal.

### API Service

```bash
SERVER_PORT=8080 \
APP_MERCHANT_DEMO_SALT_KEY=merchant-demo-secret-key \
./mvnw -pl api-service -am spring-boot:run
```

### Processor Service

```bash
SERVER_PORT=8081 \
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SPRING_DATA_REDIS_HOST=localhost \
./mvnw -pl processor-service -am spring-boot:run
```

### Ledger Writer Service

```bash
SERVER_PORT=8082 \
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/payments \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=postgres \
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/payment_audit \
./mvnw -pl ledger-writer-service -am spring-boot:run
```

### Audit Service

```bash
SERVER_PORT=8083 \
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/payment_audit \
./mvnw -pl audit-service -am spring-boot:run
```

> [!IMPORTANT]
> Outside Docker, the services do not all default to unique HTTP ports. Set `SERVER_PORT` explicitly as shown above to avoid port conflicts on `8080`.

## Enabling Or Updating Read API Access At Runtime

Instead of restarting the API with new environment variables, you can update the Redis hash used by `ReadAuthConfigService`:

```bash
docker exec -it hvtp-redis redis-cli HSET read-auth:config \
  api_keys "read-key-demo" \
  rate_limits "read-key-demo:120" \
  rate_window_seconds "60" \
  default_limit_per_window "60" \
  api_key_merchants "read-key-demo:merchant-demo"
```

## Status Endpoint Behavior

The status endpoint is backed by PostgreSQL `ledger_entries`, so keep these expectations in mind:

- `200` usually means the ledger writer has already persisted the transaction
- `404` usually means the transaction is not yet persisted, was rejected as a duplicate, or failed before ledger persistence
- `403` means the API key is valid but not allowed to view that merchant's transaction

## Working Signing Reference

If you need a code reference for the request-signing algorithm, use:

- [`performance/k6/payload.js`](/home/kaustubh/Desktop/projects/Java/high-volume-transaction-processor/performance/k6/payload.js)
- [`api-service/src/main/java/com/kaustubh/transactions/api/security/VerifySignatureFilter.java`](/home/kaustubh/Desktop/projects/Java/high-volume-transaction-processor/api-service/src/main/java/com/kaustubh/transactions/api/security/VerifySignatureFilter.java)

The formula is:

```text
SHA256_HEX_UPPER(BASE64(body) + path + timestamp + nonce + idempotencyKey + saltKey) + "###" + saltIndex
```
