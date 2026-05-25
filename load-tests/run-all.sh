#!/usr/bin/env bash
# run-all.sh — PoUW k6 load test orchestrator
#
# Runs all tests sequentially (T1 → T2 → T3 → T4 → T5 → T6 → T7),
# collects JSON summaries under results/, and exits non-zero if any test
# failed its thresholds.
#
# Usage:
#   cd load-tests
#   bash run-all.sh [options]
#
# Options:
#   --target-host URL     Base URL of the stack (default: http://localhost:80)
#   --plugin-id   ID      Plugin to use for PoUW tests (default: sha256)
#   --env-file    PATH    Source additional env vars from file before each test
#   --skip        T1,T4   Comma-separated list of test IDs to skip
#   --only        T1,T2   Run only these test IDs
#
# Requirements: k6, jq, bash ≥ 4

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCENARIOS_DIR="${SCRIPT_DIR}/k6/scenarios"
RESULTS_DIR="${SCRIPT_DIR}/results"

TARGET_HOST="${TARGET_HOST:-http://localhost:80}"
PLUGIN_ID="${PLUGIN_ID:-sha256}"
ENV_FILE=""
SKIP_TESTS=""
ONLY_TESTS=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --target-host) TARGET_HOST="$2"; shift 2 ;;
    --plugin-id)   PLUGIN_ID="$2";   shift 2 ;;
    --env-file)    ENV_FILE="$2";    shift 2 ;;
    --skip)        SKIP_TESTS="$2";  shift 2 ;;
    --only)        ONLY_TESTS="$2";  shift 2 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

mkdir -p "${RESULTS_DIR}"

# Load optional extra env file
if [[ -n "${ENV_FILE}" && -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

FAILED_TESTS=()
PASSED_TESTS=()

should_run() {
  local id="$1"
  if [[ -n "${ONLY_TESTS}" ]]; then
    [[ ",${ONLY_TESTS}," == *",${id},"* ]]
    return
  fi
  if [[ -n "${SKIP_TESTS}" ]]; then
    [[ ",${SKIP_TESTS}," != *",${id},"* ]]
    return
  fi
  return 0
}

run_k6() {
  local id="$1"
  local script="$2"
  local result="${RESULTS_DIR}/${id}.json"
  shift 2
  local extra_env=("$@")

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  Running ${id} — ${script##*/}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  local k6_args=(
    run
    -e "TARGET_HOST=${TARGET_HOST}"
    -e "PLUGIN_ID=${PLUGIN_ID}"
  )
  for e in "${extra_env[@]}"; do
    k6_args+=(-e "${e}")
  done
  k6_args+=(--out "json=${result}" "${script}")

  if k6 "${k6_args[@]}"; then
    PASSED_TESTS+=("${id}")
    echo "  ✓ ${id} PASSED"
  else
    FAILED_TESTS+=("${id}")
    echo "  ✗ ${id} FAILED (threshold violation or error)"
  fi
}

# ── T1: Clean Baseline ────────────────────────────────────────────────
if should_run T1; then
  run_k6 T1 "${SCENARIOS_DIR}/t1-clean-baseline.js"
fi

# ── T2: Full PoUW Cycle ───────────────────────────────────────────────
if should_run T2; then
  run_k6 T2 "${SCENARIOS_DIR}/t2-pow-cycle.js"
fi

# ── T3: Throughput Ramp ───────────────────────────────────────────────
if should_run T3; then
  run_k6 T3 "${SCENARIOS_DIR}/t3-throughput-ramp.js"
fi

# ── T4: Soak (60 min) ─────────────────────────────────────────────────
if should_run T4; then
  SOAK_DURATION="${SOAK_DURATION:-60m}"
  run_k6 T4 "${SCENARIOS_DIR}/t4-soak.js" "SOAK_DURATION=${SOAK_DURATION}"
fi

# ── T5: Config Comparison (shell wrapper) ────────────────────────────
if should_run T5; then
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  Running T5 — t5-config-compare.sh"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  if TARGET_HOST="${TARGET_HOST}" bash "${SCENARIOS_DIR}/t5-config-compare.sh"; then
    PASSED_TESTS+=(T5)
    echo "  ✓ T5 PASSED"
  else
    FAILED_TESTS+=(T5)
    echo "  ✗ T5 FAILED"
  fi
fi

# ── T6: /challenge Load ───────────────────────────────────────────────
if should_run T6; then
  run_k6 T6 "${SCENARIOS_DIR}/t6-challenge-load.js" \
    "VU_COUNT=${VU_COUNT:-20}" \
    "TARGET_RPS=${TARGET_RPS:-50}"
fi

# ── T7: Static Asset Load ─────────────────────────────────────────────
if should_run T7; then
  run_k6 T7 "${SCENARIOS_DIR}/t7-static-load.js" \
    "VU_COUNT=${VU_COUNT:-20}"
fi

# ── Summary ───────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Passed: ${PASSED_TESTS[*]:-none}"
echo "  Failed: ${FAILED_TESTS[*]:-none}"
echo "  Results: ${RESULTS_DIR}/"
echo ""

if [[ ${#FAILED_TESTS[@]} -gt 0 ]]; then
  echo "  ✗ One or more tests failed."
  exit 1
fi

echo "  ✓ All tests passed."
exit 0
