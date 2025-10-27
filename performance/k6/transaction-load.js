import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { appConfig, loadProfile, thresholdsConfig } from './config.js';
import { buildHeaders, buildTransactionPayload } from './payload.js';

const acceptedRequests = new Counter('accepted_requests');
const failedRequests = new Counter('failed_requests');
const status202Rate = new Rate('status_202_rate');
const appCheckRate = new Rate('app_check_rate');
const acceptedLatency = new Trend('accepted_latency_ms', true);
const endToEndRequestDuration = new Trend('end_to_end_request_duration_ms', true);
const status0 = new Counter('status_0');
const status400 = new Counter('status_400');
const status401 = new Counter('status_401');
const status403 = new Counter('status_403');
const status404 = new Counter('status_404');
const status409 = new Counter('status_409');
const status429 = new Counter('status_429');
const status500 = new Counter('status_500');
const status502 = new Counter('status_502');
const status503 = new Counter('status_503');
const status504 = new Counter('status_504');
const statusOther = new Counter('status_other');

function buildScenarios() {
  if (loadProfile.mode === 'ramp') {
    return {
      transaction_ingress_ramp: {
        executor: 'ramping-arrival-rate',
        startRate: loadProfile.startRate,
        timeUnit: loadProfile.timeUnit,
        preAllocatedVUs: loadProfile.preAllocatedVUs,
        maxVUs: loadProfile.maxVUs,
        stages: [
          { target: loadProfile.stage1Target, duration: loadProfile.stage1Duration },
          { target: loadProfile.stage2Target, duration: loadProfile.stage2Duration },
          { target: loadProfile.stage3Target, duration: loadProfile.stage3Duration },
          { target: loadProfile.stage4Target, duration: loadProfile.stage4Duration },
        ],
      },
    };
  }

  return {
    transaction_ingress_constant: {
      executor: 'constant-arrival-rate',
      rate: loadProfile.rate,
      timeUnit: loadProfile.timeUnit,
      duration: loadProfile.duration,
      preAllocatedVUs: loadProfile.preAllocatedVUs,
      maxVUs: loadProfile.maxVUs,
    },
  };
}

export const options = {
  discardResponseBodies: true,
  scenarios: buildScenarios(),
  thresholds: {
    http_req_failed: [`rate<${thresholdsConfig.httpReqFailed}`],
    checks: [`rate>${thresholdsConfig.checksRate}`],
    status_202_rate: [`rate>${thresholdsConfig.acceptedRate}`],
    app_check_rate: [`rate>${thresholdsConfig.checksRate}`],
    http_req_duration: [`p(95)<${thresholdsConfig.p95Ms}`, `p(99)<${thresholdsConfig.p99Ms}`],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export function setup() {
  return {
    url: `${appConfig.baseUrl}${appConfig.path}`,
  };
}

export default function (data) {
  const payload = buildTransactionPayload();
  const body = JSON.stringify(payload);
  const headers = buildHeaders(body, payload.idempotencyKey);

  const response = http.post(
    data.url,
    body,
    {
      headers,
      timeout: appConfig.requestTimeout,
      tags: {
        endpoint: 'create_transaction',
        test_mode: loadProfile.mode,
      },
    }
  );

  const isAccepted = response.status === 202;
  status202Rate.add(isAccepted);
  endToEndRequestDuration.add(response.timings.duration);
  trackStatus(response.status);

  const checksPassed = check(response, {
    'status is 202': (r) => r.status === 202,
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
  const summary = {
    testMode: loadProfile.mode,
    baseUrl: appConfig.baseUrl,
    path: appConfig.path,
    configuredRate: loadProfile.rate,
    configuredDuration: loadProfile.duration,
    metrics: {
      http_req_duration: data.metrics.http_req_duration,
      http_req_failed: data.metrics.http_req_failed,
      iterations: data.metrics.iterations,
      checks: data.metrics.checks,
      accepted_requests: data.metrics.accepted_requests,
      failed_requests: data.metrics.failed_requests,
      status_202_rate: data.metrics.status_202_rate,
      status_0: data.metrics.status_0,
      status_400: data.metrics.status_400,
      status_401: data.metrics.status_401,
      status_403: data.metrics.status_403,
      status_404: data.metrics.status_404,
      status_409: data.metrics.status_409,
      status_429: data.metrics.status_429,
      status_500: data.metrics.status_500,
      status_502: data.metrics.status_502,
      status_503: data.metrics.status_503,
      status_504: data.metrics.status_504,
      status_other: data.metrics.status_other,
      app_check_rate: data.metrics.app_check_rate,
      accepted_latency_ms: data.metrics.accepted_latency_ms,
      end_to_end_request_duration_ms: data.metrics.end_to_end_request_duration_ms,
    },
  };

  return {
    stdout: textSummary(summary, data),
    'performance/k6/results/summary.json': JSON.stringify(summary, null, 2),
  };
}

function metricValue(metric, field, fallback = 'n/a') {
  if (!metric || !metric.values || metric.values[field] === undefined) {
    return fallback;
  }
  return metric.values[field];
}

function textSummary(summary, raw) {
  const duration = raw.state ? raw.state.testRunDurationMs : 'n/a';
  const reqDuration = raw.metrics.http_req_duration;
  const failedRate = raw.metrics.http_req_failed;
  const iterations = raw.metrics.iterations;
  const dropped = raw.metrics.dropped_iterations;
  const accepted = raw.metrics.accepted_requests;
  const failed = raw.metrics.failed_requests;
  const acceptedRate = raw.metrics.status_202_rate;
  const status0Metric = raw.metrics.status_0;
  const status400Metric = raw.metrics.status_400;
  const status401Metric = raw.metrics.status_401;
  const status403Metric = raw.metrics.status_403;
  const status404Metric = raw.metrics.status_404;
  const status409Metric = raw.metrics.status_409;
  const status429Metric = raw.metrics.status_429;
  const status500Metric = raw.metrics.status_500;
  const status502Metric = raw.metrics.status_502;
  const status503Metric = raw.metrics.status_503;
  const status504Metric = raw.metrics.status_504;
  const statusOtherMetric = raw.metrics.status_other;

  const durationMs = typeof duration === 'number' ? duration : 0;
  const iterationCount = metricValue(iterations, 'count', 0);
  const achievedRate = durationMs > 0
    ? (iterationCount / durationMs) * 1000
    : 'n/a';

  return `
=== HVTP k6 Summary ===
mode: ${summary.testMode}
target url: ${summary.baseUrl}${summary.path}
duration_ms: ${duration}

iterations_count: ${metricValue(iterations, 'count')}
dropped_iterations: ${metricValue(dropped, 'count')}
achieved_rate_per_sec: ${typeof achievedRate === 'number' ? achievedRate.toFixed(2) : achievedRate}
accepted_requests: ${metricValue(accepted, 'count')}
failed_requests: ${metricValue(failed, 'count')}
http_failed_rate: ${metricValue(failedRate, 'rate')}
status_202_rate: ${metricValue(acceptedRate, 'rate')}

http_req_duration_avg_ms: ${metricValue(reqDuration, 'avg')}
http_req_duration_p95_ms: ${metricValue(reqDuration, 'p(95)')}
http_req_duration_p99_ms: ${metricValue(reqDuration, 'p(99)')}
http_req_duration_max_ms: ${metricValue(reqDuration, 'max')}

status_0_count: ${metricValue(status0Metric, 'count')}
status_400_count: ${metricValue(status400Metric, 'count')}
status_401_count: ${metricValue(status401Metric, 'count')}
status_403_count: ${metricValue(status403Metric, 'count')}
status_404_count: ${metricValue(status404Metric, 'count')}
status_409_count: ${metricValue(status409Metric, 'count')}
status_429_count: ${metricValue(status429Metric, 'count')}
status_500_count: ${metricValue(status500Metric, 'count')}
status_502_count: ${metricValue(status502Metric, 'count')}
status_503_count: ${metricValue(status503Metric, 'count')}
status_504_count: ${metricValue(status504Metric, 'count')}
status_other_count: ${metricValue(statusOtherMetric, 'count')}

checks_rate: ${metricValue(raw.metrics.checks, 'rate')}
`.trim();
}

function trackStatus(status) {
  switch (status) {
    case 0:
      status0.add(1);
      break;
    case 202:
      break;
    case 400:
      status400.add(1);
      break;
    case 401:
      status401.add(1);
      break;
    case 403:
      status403.add(1);
      break;
    case 404:
      status404.add(1);
      break;
    case 409:
      status409.add(1);
      break;
    case 429:
      status429.add(1);
      break;
    case 500:
      status500.add(1);
      break;
    case 502:
      status502.add(1);
      break;
    case 503:
      status503.add(1);
      break;
    case 504:
      status504.add(1);
      break;
    default:
      statusOther.add(1);
  }
}
