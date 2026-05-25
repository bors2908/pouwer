/**
 * T1 — Clean Baseline
 *
 * Goal: measure baseline latency on a clean IP lane.
 * Run with:
 *   k6 run -e TARGET_HOST=http://localhost:80 -e PROFILE=traefik-only \
 *          --out json=results/t1.json scenarios/t1-clean-baseline.js
 *
 * Env vars:
 *   TARGET_HOST  — base URL of the stack (default: http://localhost:80)
 *   PROFILE      — traefik-only | traefik-crowdsec (informational tag only)
 *   REQUEST_PATH — path to GET (default: /healthz to avoid rate-limit noise)
 */

import http from 'k6/http';
import { check } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { THRESHOLDS_BASELINE } from '../lib/thresholds.js';
import { cleanIP } from '../lib/ip-pools.js';
import { collectAllStats } from '../lib/stats.js';

const TARGET_HOST   = __ENV.TARGET_HOST   || 'http://localhost:80';
const PROFILE       = __ENV.PROFILE       || 'traefik-only';
const REQUEST_PATH  = __ENV.REQUEST_PATH  || '/healthz';

export const options = {
  scenarios: {
    baseline: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1000,
      maxDuration: '5m',
    },
  },
  thresholds: THRESHOLDS_BASELINE,
  tags: { profile: PROFILE },
};

export default function () {
  const url = `${TARGET_HOST}${REQUEST_PATH}`;
  const res = http.get(url, {
    headers: { 'X-Forwarded-For': cleanIP() },
    tags: { lane: 'clean', profile: PROFILE },
  });

  check(res, {
    'status is 2xx or 3xx': (r) => r.status >= 200 && r.status < 400,
  });
}

export function handleSummary(data) {
  const dispersion = collectAllStats(data, ['http_req_duration']);
  return { stdout: textSummary(data, { indent: ' ', enableColors: true }) + (dispersion ? '\n' + dispersion + '\n' : '') };
}
