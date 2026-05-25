/**
 * T1 — Clean Baseline
 *
 * Goal: measure Traefik/CrowdSec overhead on a clean IP lane.
 * Run with:
 *   k6 run -e TARGET_HOST=http://localhost:80 -e PROFILE=traefik-only \
 *          --out json=results/t1.json scenarios/t1-clean-baseline.js
 *
 * Env vars:
 *   TARGET_HOST  — base URL of the stack (default: http://localhost:80)
 *   PROFILE      — traefik-only | traefik-crowdsec (informational tag only)
 *   REQUEST_PATH — path to GET (default: /)
 */

import http from 'k6/http';
import { check } from 'k6';
import { THRESHOLDS_BASELINE } from '../lib/thresholds.js';
import { cleanIP } from '../lib/ip-pools.js';

const TARGET_HOST   = __ENV.TARGET_HOST   || 'http://localhost:80';
const PROFILE       = __ENV.PROFILE       || 'traefik-only';
const REQUEST_PATH  = __ENV.REQUEST_PATH  || '/';

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
