/**
 * Dispersion and deviation helpers for k6 handleSummary.
 *
 * k6 exposes per-metric aggregates in data.metrics[name].values:
 *   avg, min, max, med, p(90), p(95), p(99)
 *
 * Raw samples are not available, so we derive:
 *   - stdDev  — approximated as IQR / 1.35  (normal-distribution estimate,
 *               where IQR = p(75)–p(25); since k6 omits p25/p75 we use
 *               (p(90) – med) / 1.28 as a one-sided σ proxy)
 *   - mad     — mean absolute deviation approximated as 0.8 × stdDev
 *               (exact for a normal distribution: MAD ≈ σ × √(2/π) ≈ 0.798σ)
 *   - cv      — coefficient of variation = stdDev / avg  (dimensionless spread)
 *   - iqrEst  — estimated IQR = (p(90) – med) × 2  (symmetric approximation)
 *
 * Usage:
 *   import { metricStats, formatStats } from '../lib/stats.js';
 *
 *   export function handleSummary(data) {
 *     const s = metricStats(data, 'http_req_duration');
 *     if (s) console.log(formatStats('http_req_duration', s));
 *     return {};
 *   }
 */

/**
 * Compute dispersion stats for a single k6 Trend metric.
 *
 * @param {Object} data   - the handleSummary data object
 * @param {string} metric - metric name, e.g. 'http_req_duration'
 * @returns {{ avg, med, stdDev, mad, cv, iqrEst, p90, p95, p99, min, max } | null}
 */
export function metricStats(data, metric) {
  const m = data.metrics[metric];
  if (!m || !m.values) return null;

  const v   = m.values;
  const avg = v['avg']   ?? 0;
  const med = v['med']   ?? 0;
  const p90 = v['p(90)'] ?? 0;
  const p95 = v['p(95)'] ?? 0;
  const p99 = v['p(99)'] ?? 0;
  const min = v['min']   ?? 0;
  const max = v['max']   ?? 0;

  // σ ≈ (p90 − med) / 1.28  (one-sided normal approximation)
  const stdDev = p90 > med ? (p90 - med) / 1.28 : 0;

  // MAD ≈ 0.8 × σ  (normal-distribution identity)
  const mad = stdDev * 0.8;

  // Coefficient of variation (relative dispersion)
  const cv = avg > 0 ? stdDev / avg : 0;

  // Estimated IQR = 2 × (p90 − med)  (symmetric around median)
  const iqrEst = 2 * (p90 - med);

  return { avg, med, stdDev, mad, cv, iqrEst, p90, p95, p99, min, max };
}

/**
 * Format dispersion stats as a human-readable string.
 *
 * @param {string} label - display label
 * @param {Object} s     - result of metricStats()
 * @param {string} unit  - unit suffix (default: 'ms')
 * @returns {string}
 */
export function formatStats(label, s, unit = 'ms') {
  const f = (n) => n.toFixed(2);
  return [
    `${label}:`,
    `  avg=${f(s.avg)}${unit}  med=${f(s.med)}${unit}  min=${f(s.min)}${unit}  max=${f(s.max)}${unit}`,
    `  p90=${f(s.p90)}${unit}  p95=${f(s.p95)}${unit}  p99=${f(s.p99)}${unit}`,
    `  stdDev≈${f(s.stdDev)}${unit}  MAD≈${f(s.mad)}${unit}  CV≈${f(s.cv * 100)}%  IQR≈${f(s.iqrEst)}${unit}`,
  ].join('\n');
}

/**
 * Collect dispersion stats for every Trend metric as a single string.
 * Skips Rate and Counter metrics (they have no avg/med/p90).
 *
 * @param {Object} data       - handleSummary data
 * @param {string[]} [names]  - explicit list of metric names; if omitted, all Trend metrics are used
 * @returns {string}
 */
export function collectAllStats(data, names) {
  const keys = names ?? Object.keys(data.metrics);
  const lines = [];
  for (const key of keys) {
    const s = metricStats(data, key);
    if (s && s.p90 > 0) {
      lines.push(formatStats(key, s));
    }
  }
  return lines.join('\n');
}

/**
 * Print dispersion stats for every Trend metric found in data.metrics.
 * Skips Rate and Counter metrics (they have no avg/med/p90).
 *
 * @param {Object} data       - handleSummary data
 * @param {string[]} [names]  - explicit list of metric names; if omitted, all Trend metrics are used
 */
export function printAllStats(data, names) {
  const out = collectAllStats(data, names);
  if (out) console.log(out);
}
