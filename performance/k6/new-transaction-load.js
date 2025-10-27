import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import exec from 'k6/execution';

function envNumber(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === null || value === '') {
    return fallback;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? fallback : parsed;
}

function envString(name, fallback) {
  const value = __ENV[name];
  return value === undefined || value === null || value === '' ? fallback : value;
}

function envBool(name, fallback) {
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

const config = {
  baseUrl: envString('BASE_URL', 'http://localhost:8080'),
  path: envString('TXN_PATH', '/api/v1/transactions'),
  merchantId: envString('MERCHANT_ID', 'merchant-demo'),
  saltKey: envString('SALT_KEY', 'merchant-demo-secret-key'),
  saltIndex: envString('SALT_INDEX', '1'),
  currency: envString('CURRENCY', 'INR'),
  requestTimeout: envString('REQUEST_TIMEOUT', '5s'),
  accountCardinality: envNumber('ACCOUNT_CARDINALITY', 10000),
  fastIds: envBool('FAST_IDS', false),

  rate: envNumber('RATE', 100),
  duration: envString('DURATION', '60s'),
  timeUnit: envString('TIME_UNIT', '1s'),
  preAllocatedVUs: envNumber('PRE_ALLOCATED_VUS', 50),
  maxVUs: envNumber('MAX_VUS', 1000),

  thresholdHttpFailed: envString('THRESHOLD_HTTP_FAILED', 'rate==0'),
  thresholdStatus202: envString('THRESHOLD_STATUS_202', 'rate==1'),
  thresholdChecks: envString('THRESHOLD_CHECKS', 'rate==1'),
  thresholdP95: envString('THRESHOLD_P95', 'p(95)<1000'),
  thresholdP99: envString('THRESHOLD_P99', 'p(99)<2000'),
};

const acceptedRequests = new Counter('accepted_requests');
const failedRequests = new Counter('failed_requests');
const status202Rate = new Rate('status_202_rate');
const appCheckRate = new Rate('app_check_rate');
const acceptedLatency = new Trend('accepted_latency_ms', true);
const endToEndRequestDuration = new Trend('end_to_end_request_duration_ms', true);

export const options = {
  discardResponseBodies: false,
  scenarios: {
    transaction_ingress_constant: {
      executor: 'constant-arrival-rate',
      rate: config.rate,
      timeUnit: config.timeUnit,
      duration: config.duration,
      preAllocatedVUs: config.preAllocatedVUs,
      maxVUs: config.maxVUs,
    },
  },
  thresholds: {
    http_req_failed: [config.thresholdHttpFailed],
    status_202_rate: [config.thresholdStatus202],
    checks: [config.thresholdChecks],
    app_check_rate: [config.thresholdChecks],
    http_req_duration: [config.thresholdP95, config.thresholdP99],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

function utcTimestamp() {
  return new Date().toISOString().replace('.000Z', 'Z');
}

function accountIdForIteration(iteration) {
  const bucket = iteration % config.accountCardinality;
  return `acct-${String(bucket).padStart(8, '0')}`;
}

function buildPayload(idempotencyKey) {
  const scenarioIteration = exec.scenario.iterationInTest;

  return {
    accountId: accountIdForIteration(scenarioIteration),
    idempotencyKey,
    amount: 100.0,
    currency: config.currency,
    type: scenarioIteration % 2 === 0 ? 'DEBIT' : 'CREDIT',
  };
}

function buildCanonicalJson(payload) {
  return JSON.stringify(payload);
}

function sha256HexUpper(input) {
  return crypto.sha256(input, 'hex').toUpperCase();
}

function base64Body(bodyString) {
  return encoding.b64encode(bodyString);
}

function buildVerify(bodyString, path, timestamp, nonce, idempotencyKey) {
  const raw = `${base64Body(bodyString)}${path}${timestamp}${nonce}${idempotencyKey}${config.saltKey}`;
  const digest = sha256HexUpper(raw);
  return `${digest}###${config.saltIndex}`;
}

function bytesToHex(bytes) {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}

function uuidFromBytes(bytes) {
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = bytesToHex(bytes);
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function randomId() {
  if (config.fastIds) {
    return `${Date.now()}-${__VU}-${exec.vu.iterationInScenario}`;
  }

  if (typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  if (typeof crypto.randomBytes === 'function') {
    const buf = crypto.randomBytes(16);
    const bytes = new Uint8Array(buf);
    return uuidFromBytes(bytes);
  }

  return `${Date.now()}-${__VU}-${exec.vu.iterationInScenario}-${Math.floor(Math.random() * 1e9)}`;
}

function metricValue(metric, field, fallback = 'n/a') {
  if (!metric || !metric.values) {
    return fallback;
  }

  if (metric.values[field] !== undefined) {
    return metric.values[field];
  }

  if (field === 'count' && metric.values.value !== undefined) {
    return metric.values.value;
  }

  return fallback;
}

export function setup() {
  return {
    url: `${config.baseUrl}${config.path}`,
    path: config.path,
  };
}

export default function (data) {
  const idempotencyKey = `idem-${randomId()}`;
  const payload = buildPayload(idempotencyKey);
  const body = buildCanonicalJson(payload);

  const timestamp = utcTimestamp();
  const nonce = randomId();
  const correlationId = `corr-${randomId()}`;
  const xVerify = buildVerify(body, data.path, timestamp, nonce, idempotencyKey);

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Merchant-Id': config.merchantId,
      'X-Timestamp': timestamp,
      'X-Nonce': nonce,
      'X-Verify': xVerify,
      'Idempotency-Key': idempotencyKey,
      'X-Correlation-Id': correlationId,
    },
    timeout: config.requestTimeout,
    tags: {
      endpoint: 'create_transaction',
      auth_mode: 'phonepe_style_verify',
    },
  };

  const response = http.post(data.url, body, params);

  const isAccepted = response.status === 202;
  status202Rate.add(isAccepted);
  endToEndRequestDuration.add(response.timings.duration);

  const checksPassed = check(response, {
    'status is 202': (r) => r.status === 202,
    'response time under hard ceiling': (r) => r.timings.duration < 10000,
  });

  appCheckRate.add(checksPassed);

  if (isAccepted) {
    acceptedRequests.add(1);
    acceptedLatency.add(response.timings.duration);
  } else {
    failedRequests.add(1);
  }
}

export function handleSummary(data) {
  return {
    stdout: `
=== HVTP k6 Summary ===
mode: constant
target url: ${config.baseUrl}${config.path}

iterations_count: ${metricValue(data.metrics.iterations, 'count')}
accepted_requests: ${metricValue(data.metrics.accepted_requests, 'count')}
failed_requests: ${metricValue(data.metrics.failed_requests, 'count')}
http_failed_rate: ${metricValue(data.metrics.http_req_failed, 'rate')}
status_202_rate: ${metricValue(data.metrics.status_202_rate, 'rate')}

http_req_duration_avg_ms: ${metricValue(data.metrics.http_req_duration, 'avg')}
http_req_duration_p95_ms: ${metricValue(data.metrics.http_req_duration, 'p(95)')}
http_req_duration_p99_ms: ${metricValue(data.metrics.http_req_duration, 'p(99)')}
http_req_duration_max_ms: ${metricValue(data.metrics.http_req_duration, 'max')}
`.trim(),
  };
}
