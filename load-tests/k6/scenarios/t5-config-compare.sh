#!/usr/bin/env bash
# T5 — Config Comparison
#
# ⚠️  INCOMPLETE: Currently runs tests against a single server deployment.
# To properly compare profiles, each should deploy with different docker-compose
# configurations (traefik-only, traefik-crowdsec, etc.), but the script does not
# manage those deployments yet. This version is useful for variance testing only.
#
# Runs T1 (clean baseline) and T2 (PoUW cycle) multiple times and produces
# a p50 comparison table in Markdown.
#
# Usage:
#   cd load-tests
#   bash k6/scenarios/t5-config-compare.sh [--target-host http://localhost:80] [--core-host http://localhost:8082]
#
# Requirements: k6, grep

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOAD_TESTS_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)/load-tests"
RESULTS_DIR="${LOAD_TESTS_DIR}/results/t5"
ENVS_DIR="${LOAD_TESTS_DIR}/envs"

TARGET_HOST="${TARGET_HOST:-http://localhost:80}"
CORE_HOST="${CORE_HOST:-http://localhost:8082}"

# Allow --target-host override
while [[ $# -gt 0 ]]; do
  case "$1" in
    --target-host) TARGET_HOST="$2"; shift 2 ;;
    --core-host) CORE_HOST="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

mkdir -p "${RESULTS_DIR}"

PROFILES=(
  traefik-only
  traefik-crowdsec
  traefik-crowdsec-sha256
  traefik-crowdsec-monero
)

declare -A T1_P50
declare -A T2_P50

run_test() {
  local profile="$1"
  local test_script="$2"
  local result_file="$3"
  local env_file="${ENVS_DIR}/${profile}.env"

  # Load env file into current shell
  set -a
  # shellcheck disable=SC1090
  source "${env_file}"
  set +a

  echo "  Running ${test_script##*/} with profile=${profile} ..."
  k6 run \
    -e TARGET_HOST="${TARGET_HOST}" \
    -e PROFILE="${profile}" \
    -e CORE_HOST="${CORE_HOST}" \
    --out "json=${result_file}" \
    --quiet \
    "${test_script}" || true   # don't abort on threshold failure; collect results
}

extract_p50() {
  local result_file="$1"
  local metric="${2:-http_req_duration}"
  # k6 outputs JSONL; parse summary line containing metric name and p(50)
  grep -oP '"'"${metric}"'".*?"p\(50\)":\K[0-9.]+' "${result_file}" 2>/dev/null | head -1 || echo "N/A"
}

echo "=== T5: Config Comparison ==="
echo ""

for profile in "${PROFILES[@]}"; do
  echo "Profile: ${profile}"

  t1_result="${RESULTS_DIR}/t1-${profile}.json"
  t2_result="${RESULTS_DIR}/t2-${profile}.json"

  run_test "${profile}" "${SCRIPT_DIR}/t1-clean-baseline.js" "${t1_result}"
  run_test "${profile}" "${SCRIPT_DIR}/t2-pow-cycle.js"      "${t2_result}"

  T1_P50["${profile}"]="$(extract_p50 "${t1_result}")"
  T2_P50["${profile}"]="$(extract_p50 "${t2_result}")"
done

# Compute multipliers relative to traefik-only baseline
BASE_T1="${T1_P50[traefik-only]}"
BASE_T2="${T2_P50[traefik-only]}"

echo ""
echo "## T5 Results — p50 Multiplier Table"
echo ""
printf "| %-30s | %12s | %10s | %12s | %10s |\n" \
  "Profile" "T1 p50 (ms)" "T1 mult" "T2 p50 (ms)" "T2 mult"
printf "|%s|%s|%s|%s|%s|\n" \
  "--------------------------------" "--------------" "------------" "--------------" "------------"

for profile in "${PROFILES[@]}"; do
  t1="${T1_P50[$profile]}"
  t2="${T2_P50[$profile]}"

  if [[ "${BASE_T1}" == "N/A" || "${t1}" == "N/A" ]]; then
    t1_mult="N/A"
  else
    t1_mult="$(echo "scale=2; ${t1} / ${BASE_T1}" | bc)x"
  fi

  if [[ "${BASE_T2}" == "N/A" || "${t2}" == "N/A" ]]; then
    t2_mult="N/A"
  else
    t2_mult="$(echo "scale=2; ${t2} / ${BASE_T2}" | bc)x"
  fi

  printf "| %-30s | %12s | %10s | %12s | %10s |\n" \
    "${profile}" "${t1}" "${t1_mult}" "${t2}" "${t2_mult}"
done

echo ""
echo "Results saved to: ${RESULTS_DIR}"
