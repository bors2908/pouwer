/**
 * T4 — Soak Test
 *
 * Goal: detect memory drift, GC pressure, and slow degradation over 60 minutes.
 * Two parallel scenarios:
 *   - captcha lane: suspicious IP pool at ~50 % of max stable RPS (from T3)
 *   - clean lane:   clean IP pool at a fixed moderate rate
 *
 * Run with:
 *   k6 run -e TARGET_HOST=http://localhost:80 -e PLUGIN_ID=sha256 \
 *          -e CAPTCHA_VUS=50 -e CLEAN_VUS=20 \
 *          --out json=results/t4.json scenarios/t4-soak.js
 *
 * Env vars:
 *   TARGET_HOST    — base URL (default: http://localhost:80)
 *   PLUGIN_ID      — sha256 | monero | bitcoin (default: sha256)
 *   CAPTCHA_VUS    — VUs for captcha/suspicious lane (default: 50)
 *   CLEAN_VUS      — VUs for clean lane (default: 20)
 *   SOAK_DURATION  — total soak duration (default: 60m)
 */

import http from 'k6/http';
import { check } from 'k6';
import { THRESHOLDS_SOAK } from '../lib/thresholds.js';
import { cleanIP, suspiciousIP } from '../lib/ip-pools.js';

const TARGET_HOST   = __ENV.TARGET_HOST    || 'http://localhost:80';
const PLUGIN_ID     = __ENV.PLUGIN_ID      || 'sha256';
const CAPTCHA_VUS   = parseInt(__ENV.CAPTCHA_VUS   || '50', 10);
const CLEAN_VUS     = parseInt(__ENV.CLEAN_VUS     || '20', 10);
const SOAK_DURATION = __ENV.SOAK_DURATION          || '60m';

export const options = {
  scenarios: {
    captcha_lane: {
      executor: 'constant-vus',
      vus: CAPTCHA_VUS,
      duration: SOAK_DURATION,
      tags: { lane: 'suspicious' },
    },
    clean_lane: {
      executor: 'constant-vus',
      vus: CLEAN_VUS,
      duration: SOAK_DURATION,
      tags: { lane: 'clean' },
    },
  },
  thresholds: THRESHOLDS_SOAK,
};

export default function () {
  const isCaptcha = __ENV.SCENARIO === 'captcha_lane';
  const ip   = isCaptcha ? suspiciousIP() : cleanIP();
  const lane = isCaptcha ? 'suspicious' : 'clean';

  const res = http.get(
    `${TARGET_HOST}/challenge?pluginId=${PLUGIN_ID}`,
    {
      headers: { 'X-Forwarded-For': ip },
      tags: { lane, pluginId: PLUGIN_ID },
    },
  );

  check(res, {
    'status not 5xx': (r) => r.status < 500,
  });
}
