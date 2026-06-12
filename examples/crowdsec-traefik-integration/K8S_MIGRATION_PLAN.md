# Migration Plan: CrowdSec-Traefik-Pouwer Integration to Kubernetes

This document outlines the detailed plan to migrate the `examples/crowdsec-traefik-integration` environment from Docker Compose to Kubernetes using Helm charts.

## 1. Overview of Current Components

The current stack consists of:
- **Security & Networking**: Traefik (v3.1) with CrowdSec Bouncer plugin, CrowdSec (LAPI + Parsers).
- **Pouwer Stack**: Pouwer Server, Monero Plugin, SHA256 Plugin.
- **Crypto Infrastructure**: Monero Node (simple-monerod), P2Pool.
- **Example Apps**: Browser App, API App.

## 2. Target Kubernetes Architecture

- **Namespace**: `pouwer-system` (suggested).
- **Ingress Controller**: Traefik (installed via Helm).
- **Security Engine**: CrowdSec (installed via Helm).
- **Application Stack**: Custom Helm chart for Pouwer and example apps.
- **Storage**: PersistentVolumeClaims (PVC) for Monero blockchain data and CrowdSec database.
- **Config**: ConfigMaps and Secrets for environment variables and configuration files.

## 3. Recommended Helm Charts

We will reuse well-maintained public charts where possible:

| Component | Source | Reason |
| :--- | :--- | :--- |
| **Traefik** | `traefik/traefik` | Standard ingress controller, supports plugins via values. |
| **CrowdSec** | `crowdsecurity/crowdsec` | Official chart for LAPI and agents. |
| **Monero Node** | `bitnami/monero` (or similar) or custom | If a reliable public chart isn't found, use a standard StatefulSet. |
| **Pouwer & Apps** | Custom Chart | To bundle the specific logic and dependencies of the Pouwer stack. |

## 4. Migration Steps

### Phase 1: Infrastructure & Shared Services
1. **Prepare Kubernetes Cluster**: Ensure RBAC and a StorageClass (e.g., standard, longhorn) are available.
2. **Install CrowdSec**:
    - Deploy using `crowdsecurity/crowdsec` chart.
    - Configure LAPI.
    - Create a Secret for `CROWDSEC_BOUNCER_KEY`.
    - Persist `/var/lib/crowdsec/data` using a PVC.
3. **Install Traefik**:
    - Deploy using `traefik/traefik` chart.
    - **Plugin Configuration**: Add the `bouncer` plugin in `additionalArguments` or via `experimental.plugins`.
    - **Middleware**: Define a `Middleware` custom resource (CRD) for the CrowdSec bouncer.
    - Configure `accessLog` to output to a volume or stdout (CrowdSec agent will need to read these).

### Phase 2: Crypto & Pouwer Stack
4. **Deploy Monero Node & P2Pool**:
    - Use a **StatefulSet** for the Monero node to handle large blockchain storage.
    - Expose RPC and ZMQ services for P2Pool.
5. **Deploy Pouwer Components**:
    - Create a custom Helm chart for `pouwer-server`, `pouwer-monero-plugin`, and `pouwer-sha256-plugin`.
    - Use **Deployments** for each.
    - Use **Services** for internal communication (replacing Docker Compose service names).
    - Handle `pouwer-pages-data` using a shared `ReadWriteMany` PVC or by embedding static files into the `pouwer-server` image.

### Phase 3: Example Applications
6. **Deploy Example Apps**:
    - Add `browser-app` and `api-app` to the custom Helm chart.
    - Create **Ingress** or **IngressRoute** resources to expose them through Traefik, applying the CrowdSec middleware.

## 5. Helm Templating Strategy

### Values.yaml Design
The custom chart should be highly configurable:
- `global.domain`: e.g., `example.com`.
- `pouwer.server.image`: Repository and tag.
- `crowdsec.lapi.url`: URL of the CrowdSec service.
- `monero.enabled`: Toggle for running a local node vs. using an external one.
- `persistence.enabled`: Enable/disable PVCs for dev/prod environments.

### Shared Configuration
- Use **ConfigMaps** to inject `acquis.yaml` and Traefik dynamic configurations.
- Use **Secrets** for API keys and sensitive credentials.

## 6. Networking & Security Considerations
- **Internal Communication**: Use K8s DNS (e.g., `http://pouwer-server:8082`).
- **CrowdSec Log Acquisition**: In K8s, CrowdSec agents usually run as a DaemonSet to read container logs from the node's `/var/log/pods` path. Update `acquis.yaml` to use the `kubernetes` source instead of a flat file.
- **Traefik Middleware**: Ensure the `Middleware` CRD is applied to all public-facing Ingresses.

## 7. Verification Plan
1. Validate Helm template rendering: `helm template .`
2. Deploy to a test cluster (e.g., k3s or minikube).
3. Verify Traefik picks up the CrowdSec plugin.
4. Test the "Bouncer" functionality by simulating an attack and checking if the IP is blocked in K8s.
5. Verify Monero node sync and P2Pool connectivity.
