# k6 run examples

## Baseline 100 TPS for 60 seconds
```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e MODE=constant \
  -e RATE=100 \
  -e DURATION=60s \
  -e PRE_ALLOCATED_VUS=50 \
  -e MAX_VUS=300 \
  performance/k6/transaction-load.js
