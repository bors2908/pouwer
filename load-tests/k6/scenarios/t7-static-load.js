/**
 * T7 — Static Asset Load
 *
 * Goal: measure cold vs warm cache performance for static JS/WASM assets
 *       served via /static/{pluginId}/**.
 *
 * Run with:
 *   k6 run -e TARGET_HOST=http://localhost:80 -e PLUGIN_ID=pow-test-sha256 \
 *          -e VU_COUNT=20 --out json=results/t7.json scenarios/t7-static-load.js
 *
 * Env vars:
 *   TARGET_HOST  — base URL (default: http://localhost:80)
 *   PLUGIN_ID    — plugin whose static assets to fetch (default: pow-test-sha256)
 *   VU_COUNT     — concurrent VUs per scenario (default: 20)
 *   ASSET_PATHS  — comma-separated asset paths relative to /static/{pluginId}/
 *                  sha256 default: challenge-sha256.js
 *                  monero default: challenge-monero.js
 */

import http from 'k6/http';
import { check, group } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { Trend, Rate } from 'k6/metrics';
import { THRESHOLDS_STATIC } from '../lib/thresholds.js';
import { cleanIP } from '../lib/ip-pools.js';
import { collectAllStats } from '../lib/stats.js';

const TARGET_HOST  = __ENV.TARGET_HOST  || 'http://localhost:80';
const PLUGIN_ID    = __ENV.PLUGIN_ID    || 'pow-test-sha256';
const VU_COUNT     = parseInt(__ENV.VU_COUNT || '20', 10);
const PLUGIN_ASSET_DEFAULTS = {
  'pow-test-sha256': 'sha256/challenge-sha256.js',
  'monero-randomx':  'monero/challenge-monero.js',
};
const ASSET_PATHS  = (__ENV.ASSET_PATHS || PLUGIN_ASSET_DEFAULTS[PLUGIN_ID] || 'challenge-sha256.js').split(',');

const coldDuration = new Trend('static_cold_duration', true);
const warmDuration = new Trend('static_warm_duration', true);
const coldErrors   = new Rate('static_cold_errors');
const warmErrors   = new Rate('static_warm_errors');

export const options = {
  scenarios: {
    cold: {
      executor: 'constant-vus',
      vus: VU_COUNT,
      duration: '1m',
      tags: { cache: 'cold' },
    },
    warm: {
      executor: 'constant-vus',
      vus: VU_COUNT,
      duration: '1m',
      startTime: '70s',   // start after cold scenario finishes
      tags: { cache: 'warm' },
    },
  },
  thresholds: THRESHOLDS_STATIC,
};

export default function () {
  const isCold = __ENV.SCENARIO !== 'warm';
  const cache  = isCold ? 'cold' : 'warm';

  group(`static-${cache}`, () => {
    for (const assetPath of ASSET_PATHS) {
      const url = `${TARGET_HOST}/static/${PLUGIN_ID}/${assetPath}`;
      const headers = { 'X-Forwarded-For': cleanIP() };

      if (isCold) {
        // Bypass any proxy/CDN cache
        headers['Cache-Control'] = 'no-cache, no-store';
        headers['Pragma']        = 'no-cache';
      }

      const res = http.get(url, {
        headers,
        tags: { cache, pluginId: PLUGIN_ID, asset: assetPath },
      });

      const ok = res.status === 200;
      check(res, { [`${cache} ${assetPath} status 200`]: () => ok });

      if (isCold) {
        coldDuration.add(res.timings.duration);
        coldErrors.add(!ok);
      } else {
        warmDuration.add(res.timings.duration);
        warmErrors.add(!ok);
      }
    }
  });
}

export function handleSummary(data) {
  const cold = data.metrics['static_cold_duration'];
  const warm = data.metrics['static_warm_duration'];
  let extra = '';
  if (cold && warm) {
    const ratio = (cold.values['p(95)'] / (warm.values['p(95)'] || 1)).toFixed(2);
    extra += `Cold/warm p95 ratio: ${ratio}x\n`;
  }
  const dispersion = collectAllStats(data, ['static_cold_duration', 'static_warm_duration', 'http_req_duration']);
  if (dispersion) extra += dispersion + '\n';
  return { stdout: textSummary(data, { indent: ' ', enableColors: true }) + (extra ? '\n' + extra : '') };
}
