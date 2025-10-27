export function envNumber(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === null || value === '') {
    return fallback;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? fallback : parsed;
}

export function envString(name, fallback) {
  const value = __ENV[name];
  return value === undefined || value === null || value === '' ? fallback : value;
}

export function envBool(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === null || value === '') {
    return fallback;
  }

  const normalized = String(value).trim().toLowerCase();
  if (['1', 'true', 'yes', 'y', 'on'].includes(normalized)) {
    return true;
  }
  if (['0', 'false', 'no', 'n', 'off'].includes(normalized)) {
    return false;
  }
  return fallback;
}

export const appConfig = {
  baseUrl: envString('BASE_URL', 'http://localhost:8080'),
  path: envString('TXN_PATH', '/api/v1/transactions'),
  currency: envString('CURRENCY', 'INR'),
  accountPrefix: envString('ACCOUNT_PREFIX', 'acct'),
  idempotencyPrefix: envString('IDEMPOTENCY_PREFIX', 'idem'),
  correlationPrefix: envString('CORRELATION_PREFIX', 'corr'),
  requestTimeout: envString('REQUEST_TIMEOUT', '5s'),
  accountCardinality: envNumber('ACCOUNT_CARDINALITY', 10000),
  merchantId: envString('MERCHANT_ID', 'merchant-demo'),
  saltKey: envString('SALT_KEY', 'merchant-demo-secret-key'),
  saltIndex: envString('SALT_INDEX', '1'),
  fastIds: envBool('FAST_IDS', false),
};

export const loadProfile = {
  mode: envString('MODE', 'constant'),
  rate: envNumber('RATE', 100),
  duration: envString('DURATION', '60s'),
  timeUnit: envString('TIME_UNIT', '1s'),
  preAllocatedVUs: envNumber('PRE_ALLOCATED_VUS', 50),
  maxVUs: envNumber('MAX_VUS', 1000),

  startRate: envNumber('START_RATE', 100),
  stage1Target: envNumber('STAGE1_TARGET', 500),
  stage1Duration: envString('STAGE1_DURATION', '1m'),
  stage2Target: envNumber('STAGE2_TARGET', 1000),
  stage2Duration: envString('STAGE2_DURATION', '2m'),
  stage3Target: envNumber('STAGE3_TARGET', 1000),
  stage3Duration: envString('STAGE3_DURATION', '2m'),
  stage4Target: envNumber('STAGE4_TARGET', 100),
  stage4Duration: envString('STAGE4_DURATION', '30s'),
};

export const thresholdsConfig = {
  httpReqFailed: envNumber('THRESHOLD_HTTP_REQ_FAILED', 0.01),
  checksRate: envNumber('THRESHOLD_CHECKS_RATE', 0.99),
  p95Ms: envNumber('THRESHOLD_P95_MS', 250),
  p99Ms: envNumber('THRESHOLD_P99_MS', 500),
  acceptedRate: envNumber('THRESHOLD_ACCEPTED_RATE', 0.99),
};
