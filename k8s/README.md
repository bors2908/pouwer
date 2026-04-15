# CrowdSec edge protection PoC (Kubernetes + Traefik)

This bundle deploys a split web edge with:

- **Browser host:** `example.com` (placeholder fake-login page + CrowdSec captcha remediation).
- **API host:** `api.example.com` (401 for unauthenticated traffic + 429 throttling middleware).
- **CrowdSec Security Engine:** installed via official Helm chart, ingesting Traefik logs from `/var/log/containers`.
- **Traefik remediation:** CrowdSec bouncer middleware in **stream mode** (local decision cache polling).

## Layout

- `k8s/crowdsec/values.yaml` - CrowdSec Helm values.
- `k8s/crowdsec/profiles.yaml` - ordered captcha + fallback decision profiles.
- `k8s/crowdsec/scenarios/*.yaml` - browser/API HTTP abuse scenarios.
- `k8s/traefik/values.yaml` - Traefik Helm values (plugin + access logs).
- `k8s/edge-poc/` - Helm chart for demo services, routes, and middlewares.
- `k8s/EXTENSION_POW.md` - challenge swap extension point (captcha -> custom PoW).
- `k8s/kind/` - local kind+registry helper scripts/config.

## 0) One-command setup (recommended)

```bash
./k8s/deploy-all.sh
```

This script runs the full flow below (kind registry + cluster, Traefik CRDs, Traefik, CrowdSec, and `edge-poc`) and avoids the Windows CRD pipe encoding issue by applying CRDs from chart files.

## 1) Optional local dev cluster/bootstrap

```bash
cd k8s/kind
./create-local-registry.sh
./create-local-cluster.sh
```

## 2) Install Traefik (gateway + CrowdSec plugin runtime)

```bash
helm repo add traefik https://traefik.github.io/charts
helm repo update
helm pull traefik/traefik --version 39.0.7 --untar --untardir ./out
kubectl apply -f ./out/traefik/crds/
helm upgrade --install traefik traefik/traefik --version 39.0.7 -n traefik --create-namespace -f k8s/traefik/values.yaml --skip-crds
```

## 3) Install CrowdSec Security Engine (official chart)

Set one shared bouncer key value in:

- `k8s/crowdsec/values.yaml` (`lapi.env[0].value`)
- `k8s/edge-poc/values.yaml` (`crowdsec.lapiKey`)
- `k8s/edge-poc/values.yaml` trusted IP placeholders if you want explicit internal-network bypasses

Then install:

```bash
helm repo add crowdsec https://crowdsecurity.github.io/helm-charts
helm repo update
helm upgrade --install crowdsec crowdsec/crowdsec -n crowdsec --create-namespace -f k8s/crowdsec/values.yaml \
  --set-file "config.profiles\\.yaml=k8s/crowdsec/profiles.yaml" \
  --set-file "config.scenarios.browser-login-grace\\.yaml=k8s/crowdsec/scenarios/browser-login-grace.yaml" \
  --set-file "config.scenarios.api-burst\\.yaml=k8s/crowdsec/scenarios/api-burst.yaml"
```

## 4) Install demo edge workloads

```bash
helm upgrade --install edge-poc ./k8s/edge-poc -n edge-poc --create-namespace
```

Add local host mapping:

```text
127.0.0.1 example.com
127.0.0.1 api.example.com
```

## 5) Smoke tests

Traefik is reachable on host port `8080` with the provided kind config:

```bash
# Browser placeholder
curl -i -H "Host: example.com" http://127.0.0.1:8080/

# Health bypass (no remediation/auth chain)
curl -i -H "Host: example.com" http://127.0.0.1:8080/healthz
curl -i -H "Host: api.example.com" http://127.0.0.1:8080/healthz

# API unauthenticated -> 401
curl -i -H "Host: api.example.com" http://127.0.0.1:8080/v1/ping

# API authenticated -> 200
curl -i -H "Host: api.example.com" -H "Authorization: Bearer demo" http://127.0.0.1:8080/v1/ping
```

## 6) Trigger/observe edge behavior

```bash
# Trigger API middleware throttle (429)
for i in $(seq 1 100); do curl -s -o /dev/null -H "Host: api.example.com" -H "Authorization: Bearer demo" http://127.0.0.1:8080/v1/ping; done

# Create a test captcha decision manually (replace with your client IP)
kubectl -n crowdsec exec deploy/crowdsec-lapi -- cscli decisions add --ip 10.244.0.1 --type captcha --duration 30m

# Observe logs and metrics
kubectl -n traefik logs deploy/traefik -f
kubectl -n crowdsec exec deploy/crowdsec-lapi -- cscli metrics show
kubectl -n crowdsec exec deploy/crowdsec-lapi -- cscli alerts list -n 20
```

## Notes on trust and policy split

- Browser and API are isolated by host and middleware chain (`example.com` vs `api.example.com`).
- Browser challenge success does not grant API access: API still requires authorization header and has its own middleware chain.
- Trusted IP bypasses are wired separately for both hosts, but the defaults use documentation-only CIDRs so local smoke tests still exercise remediation and auth.
- `/healthz` has an explicit bypass route for both hosts.
