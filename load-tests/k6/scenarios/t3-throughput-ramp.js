/**
 * T3 — Throughput Ramp
 *
 * Goal: find the saturation point (max stable RPS) before p95 > 10 s or
 *       error rate > 1 %.  Uses suspicious IP pool only.
 *
 * Run with:
 *   k6 run -e TARGET_HOST=http://localhost:80 -e CORE_HOST=http://localhost:8082 \
 *          -e PLUGIN_ID=pow-test-sha256 \
 *          --out json=results/t3.json scenarios/t3-throughput-ramp.js
 *
 * Env vars:
 *   TARGET_HOST  — base URL for static/browser endpoints (default: http://localhost:80)
 *   CORE_HOST    — base URL for /challenge (default: TARGET_HOST)
 *   PLUGIN_ID    — pow-test-sha256 | monero-randomx | bitcoin-rpc-sha256 (default: pow-test-sha256)
 *   MAX_VUS      — maximum VUs to ramp to (default: 200)
 *   STEP_VUS     — VU increment per stage (default: 20)
 *   STEP_DURATION — duration of each stage (default: 30s)
 */

import http from 'k6/http';
import { check } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { THRESHOLDS_RAMP } from '../lib/thresholds.js';
import { suspiciousIP } from '../lib/ip-pools.js';
import { collectAllStats } from '../lib/stats.js';

const TARGET_HOST    = __ENV.TARGET_HOST     || 'http://localhost:80';
const CORE_HOST      = __ENV.CORE_HOST       || TARGET_HOST;
const PLUGIN_ID      = __ENV.PLUGIN_ID       || 'pow-test-sha256';
const MAX_VUS        = parseInt(__ENV.MAX_VUS        || '200', 10);
const STEP_VUS       = parseInt(__ENV.STEP_VUS       || '20',  10);
const STEP_DURATION  = __ENV.STEP_DURATION           || '30s';

// Build ramping-vus stages: 0→20, 20→40, …, up to MAX_VUS, then back to 0
function buildStages() {
  const stages = [];
  for (let vus = STEP_VUS; vus <= MAX_VUS; vus += STEP_VUS) {
    stages.push({ duration: STEP_DURATION, target: vus });
  }
  // Hold at peak for one extra stage then ramp down
  stages.push({ duration: STEP_DURATION, target: MAX_VUS });
  stages.push({ duration: '10s', target: 0 });
  return stages;
}

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: buildStages(),
      gracefulRampDown: '10s',
      tags: { lane: 'suspicious' },
    },
  },
  thresholds: THRESHOLDS_RAMP,
};

export default function () {
  const res = http.get(
    `${CORE_HOST}/challenge?pluginId=${PLUGIN_ID}`,
    {
      headers: { 'X-Forwarded-For': suspiciousIP() },
      tags: { lane: 'suspicious', pluginId: PLUGIN_ID },
    },
  );

  check(res, {
    'status not 5xx': (r) => r.status < 500,
  });
}

export function handleSummary(data) {
  // Report the peak RPS observed during the run
  const reqs = data.metrics['http_reqs'];
  if (reqs) {
    console.log(`Peak RPS (rate): ${reqs.values.rate.toFixed(2)} req/s`);
  }
  const dispersion = collectAllStats(data, ['http_req_duration']);
  // Always output full stats to stdout, even when the test was aborted by a threshold
  return { stdout: textSummary(data, { indent: ' ', enableColors: true }) + (dispersion ? '\n' + dispersion + '\n' : '') };
}
