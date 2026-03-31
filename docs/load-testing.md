# Load Testing

The repository includes k6 script under `performance/k6` for driving the signed transaction ingress endpoint.

## Available Scripts

| Script | Purpose |
| --- | --- |
| `performance/k6/transaction-load.js` | Constant-arrival script that includes `callback_url` in the payload |

## What The Scripts Handle For You

The k6 scripts already generate:

- `Idempotency-Key`
- `X-Correlation-Id`
- `X-Timestamp`
- `X-Nonce`
- `X-Verify`

You usually only need to provide the correct base URL and merchant secret if you changed the defaults.

## Common Environment Variables

| Variable | Default | Meaning |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:8080` | API base URL |
| `TXN_PATH` | `/api/v1/transactions` | Request path |
| `MERCHANT_ID` | `merchant-demo` | Merchant ID header |
| `SALT_KEY` | `merchant-demo-secret-key` | Secret used to compute `X-Verify` |
| `SALT_INDEX` | `1` | Salt index suffix in `X-Verify` |
| `RATE` | `100` | Constant-arrival target requests per second |
| `DURATION` | `60s` | Test duration |
| `PRE_ALLOCATED_VUS` | `50` | Pre-allocated virtual users |
| `MAX_VUS` | `1000` | Maximum virtual users |
| `MODE` | `constant` | `constant` or `ramp` for `transaction-load.js` |
| `ACCOUNT_CARDINALITY` | `10000` | Number of account buckets used during the run |
| `FAST_IDS` | `false` | Uses cheaper ID generation for very high request rates |
| `CALLBACK_URL` | script-specific | Webhook target for the webhook variant |

## Example Commands

### Constant 1000 RPS

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e MODE=constant \
  -e RATE=1000 \
  -e DURATION=60s \
  -e PRE_ALLOCATED_VUS=200 \
  -e MAX_VUS=1000 \
  performance/k6/transaction-load.js
```

### Ramp Profile

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e MODE=ramp \
  -e START_RATE=100 \
  -e STAGE1_TARGET=500 \
  -e STAGE1_DURATION=1m \
  -e STAGE2_TARGET=1000 \
  -e STAGE2_DURATION=2m \
  -e STAGE3_TARGET=1000 \
  -e STAGE3_DURATION=2m \
  -e STAGE4_TARGET=100 \
  -e STAGE4_DURATION=30s \
  performance/k6/transaction-load.js
```

### Webhook Variant

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e CALLBACK_URL=http://localhost:9090/webhook \
  -e RATE=100 \
  -e DURATION=60s \
  performance/k6/transaction-load.js
```

## Output And Interpretation

The scripts track:

- accepted request count
- failed request count
- HTTP failure rate
- `202` rate
- end-to-end request duration at the HTTP layer

Important interpretation rule:

- a `202` means the API accepted the request and published it for asynchronous processing
- it does not guarantee that the processor, ledger writer, and audit service have already finished downstream work

That is why the README performance chart is described as ingress throughput, not end-to-end throughput.

## Practical Tips

- Keep `SALT_KEY` aligned with `APP_MERCHANT_DEMO_SALT_KEY`.
- Increase `ACCOUNT_CARDINALITY` if you want to spread more requests across account keys.
- Turn on `FAST_IDS=true` when pushing high arrival rates to reduce client-side ID generation overhead.
- Use the webhook script only when you have a callback receiver available; delivery is best-effort on the server side.
