/**
 * T6 — /challenge Endpoint Load
 *
 * Goal: find the safe operating range for the /challenge endpoint under
 *       increasing VU/RPS pressure from both clean and suspicious IP pools.
 *
 * Run with:
 *   k6 run -e TARGET_HOST=http://localhost:80 -e CORE_HOST=http://localhost:8082 \
 *          -e VU_COUNT=20 -e TARGET_RPS=50 -e PLUGIN_ID=sha256 \
 *          --out json=results/t6.json scenarios/t6-challenge-load.js
 *
 * Env vars:
 *   TARGET_HOST — base URL for static/browser endpoints (default: http://localhost:80)
 *   CORE_HOST   — base URL for /challenge (default: TARGET_HOST)
 *   VU_COUNT    — number of virtual users (default: 20)
 *   TARGET_RPS  — target requests per second (default: 50)
 *   PLUGIN_ID   — pluginId query param (default: sha256)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { THRESHOLDS_CHALLENGE } from '../lib/thresholds.js';
import { cleanIP, suspiciousIP } from '../lib/ip-pools.js';

const TARGET_HOST = __ENV.TARGET_HOST || 'http://localhost:80';
const CORE_HOST   = __ENV.CORE_HOST   || TARGET_HOST;
const VU_COUNT    = parseInt(__ENV.VU_COUNT   || '20', 10);
const TARGET_RPS  = parseInt(__ENV.TARGET_RPS || '50', 10);
const PLUGIN_ID   = __ENV.PLUGIN_ID   || 'sha256';

// Pace each VU so the aggregate approaches TARGET_RPS
const SLEEP_S = VU_COUNT / TARGET_RPS;

export const options = {
  scenarios: {
    challenge_clean: {
      executor: 'constant-vus',
      vus: Math.ceil(VU_COUNT / 2),
      duration: '2m',
      tags: { lane: 'clean' },
    },
    challenge_suspicious: {
      executor: 'constant-vus',
      vus: Math.floor(VU_COUNT / 2),
      duration: '2m',
      tags: { lane: 'suspicious' },
    },
  },
  thresholds: THRESHOLDS_CHALLENGE,
};

export default function () {
  const isSuspicious = __ENV.SCENARIO === 'challenge_suspicious';
  const ip   = isSuspicious ? suspiciousIP() : cleanIP();
  const lane = isSuspicious ? 'suspicious' : 'clean';

  const url = `${CORE_HOST}/challenge?pluginId=${PLUGIN_ID}`;
  const res = http.get(url, {
    headers: { 'X-Forwarded-For': ip },
    tags: { endpoint: 'challenge', lane, pluginId: PLUGIN_ID },
  });

  check(res, {
    'challenge status 200': (r) => r.status === 200,
  });

  sleep(SLEEP_S);
}
