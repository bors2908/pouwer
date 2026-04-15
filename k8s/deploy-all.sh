#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
KIND_DIR="$SCRIPT_DIR/kind"
TRAEFIK_VALUES="$SCRIPT_DIR/traefik/values.yaml"
CROWDSEC_VALUES="$SCRIPT_DIR/crowdsec/values.yaml"
EDGE_POC_DIR="$SCRIPT_DIR/edge-poc"

TRAEFIK_CHART_VERSION="${TRAEFIK_CHART_VERSION:-39.0.7}"
TRAEFIK_CHART_REF="traefik/traefik"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

echo "Checking prerequisites..."
require_cmd docker
require_cmd kind
require_cmd kubectl
require_cmd helm

echo "Creating/ensuring local registry and kind cluster..."
"$KIND_DIR/create-local-registry.sh"
"$KIND_DIR/create-local-cluster.sh"

echo "Adding/updating Helm repos..."
helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true
helm repo add crowdsec https://crowdsecurity.github.io/helm-charts >/dev/null 2>&1 || true
helm repo update

tmpdir=$(mktemp -d)
cleanup() {
  rm -rf "$tmpdir"
}
trap cleanup EXIT

echo "Pulling Traefik chart (v$TRAEFIK_CHART_VERSION) and applying CRDs from files..."
helm pull "$TRAEFIK_CHART_REF" --version "$TRAEFIK_CHART_VERSION" --untar --untardir "$tmpdir"
kubectl apply -f "$tmpdir/traefik/crds/"

echo "Installing/upgrading Traefik with --skip-crds..."
helm upgrade --install traefik "$TRAEFIK_CHART_REF" \
  --version "$TRAEFIK_CHART_VERSION" \
  -n traefik --create-namespace \
  -f "$TRAEFIK_VALUES" \
  --skip-crds

echo "Installing/upgrading CrowdSec..."
helm upgrade --install crowdsec crowdsec/crowdsec \
  -n crowdsec --create-namespace \
  -f "$CROWDSEC_VALUES" \
  --set-file "config.profiles\\.yaml=$SCRIPT_DIR/crowdsec/profiles.yaml" \
  --set-file "config.scenarios.browser-login-grace\\.yaml=$SCRIPT_DIR/crowdsec/scenarios/browser-login-grace.yaml" \
  --set-file "config.scenarios.api-burst\\.yaml=$SCRIPT_DIR/crowdsec/scenarios/api-burst.yaml"

echo "Installing/upgrading edge-poc workloads..."
helm upgrade --install edge-poc "$EDGE_POC_DIR" -n edge-poc --create-namespace

echo
echo "Deployment complete."
echo "Add these host entries if not already present:"
echo "  127.0.0.1 example.com"
echo "  127.0.0.1 api.example.com"
echo
echo "Quick checks:"
echo "  curl -i -H 'Host: example.com' http://127.0.0.1:8080/"
echo "  curl -i -H 'Host: api.example.com' -H 'Authorization: Bearer demo' http://127.0.0.1:8080/v1/ping"
