/**
 * Shared k6 threshold definitions.
 *
 * Import the preset that matches your test's SLO, or compose your own
 * by spreading the exported objects into the `thresholds` option.
 *
 * Usage:
 *   import { THRESHOLDS_BASELINE, THRESHOLDS_RAMP } from '../lib/thresholds.js';
 *   export const options = { thresholds: THRESHOLDS_BASELINE };
 */

/** T1 — clean baseline: tight latency, zero errors */
export const THRESHOLDS_BASELINE = {
  'http_req_duration{lane:clean}': ['p(99)<200'],
  'http_req_failed{lane:clean}':   ['rate<0.001'],
};

/** T2 — full PoUW cycle: end-to-end p95 < 5 s, zero errors on challenge; 422 allowed on validate */
export const THRESHOLDS_POW_CYCLE = {
  'http_req_duration{scenario:challenge}': ['p(95)<5000'],
  'http_req_duration{scenario:validate}':  ['p(95)<5000'],
  'http_req_failed{scenario:challenge}':   ['rate<0.001'],
};

/** T3 — throughput ramp: stop criteria (abortOnFail) */
export const THRESHOLDS_RAMP = {
  'http_req_duration{lane:suspicious}': [{ threshold: 'p(95)<10000', abortOnFail: true }],
  'http_req_failed':                    [{ threshold: 'rate<0.01',   abortOnFail: true }],
};

/** T4 — soak: relaxed latency, low error budget */
export const THRESHOLDS_SOAK = {
  'http_req_duration': ['p(95)<10000'],
  'http_req_failed':   ['rate<0.01'],
};

/** T6 — /challenge load: p95 < 500 ms */
export const THRESHOLDS_CHALLENGE = {
  'http_req_duration{endpoint:challenge}': ['p(95)<500'],
  'http_req_failed{endpoint:challenge}':   ['rate<0.01'],
};

/** T7 — static asset load: warm cache p95 < 100 ms */
export const THRESHOLDS_STATIC = {
  'http_req_duration{cache:warm}': ['p(95)<100'],
  'http_req_duration{cache:cold}': ['p(95)<2000'],
  'http_req_failed':               ['rate<0.01'],
};
