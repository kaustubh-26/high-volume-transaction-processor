import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import exec from 'k6/execution';
import { appConfig } from './config.js';

function pad(value, size) {
  const str = String(value);
  if (str.length >= size) {
    return str;
  }
  return '0'.repeat(size - str.length) + str;
}

function accountIdForIteration(iteration) {
  const bucket = iteration % appConfig.accountCardinality;
  return `${appConfig.accountPrefix}-${pad(bucket, 8)}`;
}

export function buildTransactionPayload() {
  const scenarioIteration = exec.scenario.iterationInTest;
  const vuIteration = exec.vu.iterationInScenario;
  const now = Date.now();

  const accountId = accountIdForIteration(scenarioIteration);
  const uniqueSuffix = `${now}-${__VU}-${vuIteration}-${scenarioIteration}`;

  return {
    accountId,
    idempotencyKey: `${appConfig.idempotencyPrefix}-${uniqueSuffix}`,
    amount: '100.00',
    currency: appConfig.currency,
    type: scenarioIteration % 2 === 0 ? 'CREDIT' : 'DEBIT',
  };
}

function utcTimestamp() {
  return new Date().toISOString().replace('.000Z', 'Z');
}

function sha256HexUpper(input) {
  return crypto.sha256(input, 'hex').toUpperCase();
}

function base64Body(bodyString) {
  return encoding.b64encode(bodyString);
}

function buildVerify(bodyString, path, timestamp, nonce, idempotencyKey) {
  const raw = `${base64Body(bodyString)}${path}${timestamp}${nonce}${idempotencyKey}${appConfig.saltKey}`;
  const digest = sha256HexUpper(raw);
  return `${digest}###${appConfig.saltIndex}`;
}

function bytesToHex(bytes) {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}

function uuidFromBytes(bytes) {
  // RFC 4122 v4 UUID
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = bytesToHex(bytes);
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function fastId() {
  return `${Date.now()}-${__VU}-${exec.vu.iterationInScenario}`;
}

function randomId() {
  if (appConfig.fastIds) {
    return fastId();
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

export function buildHeaders(bodyString, idempotencyKey) {
  const correlationId = `${appConfig.correlationPrefix}-${randomId()}`;
  const timestamp = utcTimestamp();
  const nonce = randomId();
  const xVerify = buildVerify(bodyString, appConfig.path, timestamp, nonce, idempotencyKey);

  return {
    'Content-Type': 'application/json',
    'X-Correlation-Id': correlationId,
    'X-Merchant-Id': appConfig.merchantId,
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'Idempotency-Key': idempotencyKey,
    'X-Verify': xVerify,
  };
}
