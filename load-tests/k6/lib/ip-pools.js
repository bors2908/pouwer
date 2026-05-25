/**
 * IP pool helpers for k6 load tests.
 *
 * Clean pool  — RFC 5737 TEST-NET-1 (192.0.2.x): never routed, safe for
 *               X-Forwarded-For injection. Whitelisted in CrowdSec's parser
 *               stage (whitelists.yaml) so no captcha/ban decisions are ever
 *               issued against these IPs.
 * Suspicious pool — RFC 5737 TEST-NET-2 (198.51.100.x): used to simulate
 *               traffic that CrowdSec may flag / challenge.
 *
 * Usage:
 *   import { cleanIP, suspiciousIP } from '../lib/ip-pools.js';
 *   http.get(url, { headers: { 'X-Forwarded-For': cleanIP() } });
 */

const CLEAN_BASE      = '192.0.2.';      // TEST-NET-1 — RFC 5737
const SUSPICIOUS_BASE = '198.51.100.';   // TEST-NET-2 — RFC 5737

// Each VU gets its own JS runtime in k6, so module-level state is per-VU.
// Seed the starting offset from __VU (1-based) so that concurrent VUs begin
// at different positions in the pool and never all send the same IP at once.
const _vuOffset = (typeof __VU !== 'undefined' ? __VU - 1 : 0);

let _cleanIdx      = _vuOffset % 254;
let _suspiciousIdx = _vuOffset % 254;

/** Returns the next clean IP in round-robin order (.1 – .254). */
export function cleanIP() {
  _cleanIdx = (_cleanIdx % 254) + 1;
  return CLEAN_BASE + _cleanIdx;
}

/** Returns the next suspicious IP in round-robin order (.1 – .254). */
export function suspiciousIP() {
  _suspiciousIdx = (_suspiciousIdx % 254) + 1;
  return SUSPICIOUS_BASE + _suspiciousIdx;
}

/**
 * Returns an X-Forwarded-For header value for the given lane.
 * @param {'clean'|'suspicious'} lane
 */
export function forwardedForHeader(lane) {
  return lane === 'suspicious' ? suspiciousIP() : cleanIP();
}
