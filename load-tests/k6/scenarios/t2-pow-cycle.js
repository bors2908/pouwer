/**
 * T2 — Full PoUW Cycle
 *
 * Goal: measure end-to-end challenge → solve → validate latency per plugin.
 *
 * SHA-256: full cycle (GET /challenge → JS solver → POST /validate).
 * Monero/Bitcoin: challenge issuance + POST /validate with mock result
 *   (real solve is infeasible in k6; documents network round-trip latency only).
 *
 * Run with:
 *   k6 run -e TARGET_HOST=http://localhost:80 -e CORE_HOST=http://localhost:8082 \
 *          -e PLUGIN_ID=pow-test-sha256 \
 *          --out json=results/t2.json scenarios/t2-pow-cycle.js
 *
 * Env vars:
 *   TARGET_HOST — base URL for static/browser endpoints (default: http://localhost:80)
 *   CORE_HOST   — base URL for /challenge and /validate (default: TARGET_HOST)
 *   PLUGIN_ID   — pow-test-sha256 | monero-randomx | bitcoin-rpc-sha256 (default: pow-test-sha256)
 *   ITERATIONS  — iterations per VU (default: 100)
 */

import http from 'k6/http';
import { check, group } from 'k6';
import { THRESHOLDS_POW_CYCLE } from '../lib/thresholds.js';
import { solveSha256 } from '../lib/sha256.js';
import { cleanIP } from '../lib/ip-pools.js';

const TARGET_HOST = __ENV.TARGET_HOST || 'http://localhost:80';
const CORE_HOST   = __ENV.CORE_HOST   || TARGET_HOST;
const PLUGIN_ID   = __ENV.PLUGIN_ID   || 'pow-test-sha256';
const ITERATIONS  = parseInt(__ENV.ITERATIONS || '100', 10);

export const options = {
  scenarios: {
    pow_cycle: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: ITERATIONS,
      maxDuration: '30m',
    },
  },
  thresholds: THRESHOLDS_POW_CYCLE,
};

export default async function () {
  const ip = cleanIP();
  const headers = { 'X-Forwarded-For': ip, 'Content-Type': 'application/json' };

  let task;

  // Step 1: GET /challenge
  group('challenge', () => {
    const res = http.get(
      `${CORE_HOST}/challenge?pluginId=${PLUGIN_ID}`,
      { headers, tags: { scenario: 'challenge', pluginId: PLUGIN_ID } },
    );
    check(res, { 'challenge 200': (r) => r.status === 200 });
    if (res.status === 200) {
      task = res.json();
    }
  });

  if (!task) return;

  const startMs = Date.now();
  let solveResult = null;

  // Step 2: Solve (SHA-256 only; Monero/Bitcoin use mock)
  if (PLUGIN_ID === 'pow-test-sha256') {
    group('solve', async () => {
      try {
        solveResult = await solveSha256(task.payload);
      } catch (e) {
        console.error(`SHA-256 solve failed: ${e.message}`);
      }
    });
  } else {
    // Monero/Bitcoin: mock result — documents issuance + validate round-trip only
    solveResult = { nonce: 0, hashHex: '0000000000000000000000000000000000000000000000000000000000000000' };
    console.log(`[${PLUGIN_ID}] Skipping real solve — using mock result (latency characterization only)`);
  }

  if (!solveResult) return;

  const durationMs = Date.now() - startMs;

  // Step 3: POST /validate
  group('validate', () => {
    const body = JSON.stringify({
      jobId:     task.jobId,
      pluginId:  task.pluginId,
      payload:   solveResult,
      durationMs,
      attempts:  1,
    });

    const res = http.post(
      `${CORE_HOST}/validate`,
      body,
      { headers, tags: { scenario: 'validate', pluginId: PLUGIN_ID } },
    );

    check(res, {
      'validate accepted (200)':   (r) => r.status === 200,
      'validate not server error': (r) => r.status < 500,
    });
  });
}
