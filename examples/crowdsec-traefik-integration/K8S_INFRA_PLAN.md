# Kubernetes Infrastructure Plan
## CrowdSec-Traefik-Pouwer Migration — Infrastructure Track

> **Scope**: This document covers everything from zero to a running local cluster, fully prepped for the application migration. It does not cover application-layer Helm charts (Pouwer, example apps) — those are a separate track.

---

## 0. Prerequisites

Before touching Kubernetes, ensure the following are installed on your workstation:

| Tool | Minimum Version | Install |
|------|----------------|---------|
| Docker Desktop / Docker Engine | 24.x+ | https://docs.docker.com/get-docker/ |
| `kubectl` | 1.29+ | `brew install kubectl` / official binary |
| `helm` | 3.14+ | See Section 2 |
| `kind` | 0.23+ | See Section 1 |
| `kubectx` + `kubens` | latest | `brew install kubectx` (optional, quality of life) |

---

## 1. Local Cluster Setup (kind)

`kind` (Kubernetes IN Docker) is the recommended local cluster for this migration. It supports multi-node topologies, LoadBalancer exposure, and is the closest to a real cluster without the overhead of minikube's VM.

### 1.1 Install kind

```bash
# macOS
brew install kind

# Linux
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.23.0/kind-linux-amd64
chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind

# Verify
kind version
```

### 1.2 Cluster Configuration File

Create `kind-cluster.yaml`. This config:
- Sets up a **1 control plane + 2 worker** topology (mirrors a real small cluster).
- Exposes ports 80 and 443 on localhost so Traefik can be reached directly.
- Mounts a local path for potential PV testing.

```yaml
# kind-cluster.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: pouwer-dev

nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP

  - role: worker
    extraMounts:
      - hostPath: /tmp/pouwer-data
        containerPath: /data

  - role: worker

networking:
  podSubnet: "10.244.0.0/16"
  serviceSubnet: "10.96.0.0/12"
```

### 1.3 Create and Verify the Cluster

```bash
# Create the cluster
kind create cluster --config kind-cluster.yaml

# Verify nodes are Ready
kubectl get nodes
# Expected output:
# NAME                        STATUS   ROLES           AGE
# pouwer-dev-control-plane    Ready    control-plane   Xm
# pouwer-dev-worker           Ready    <none>          Xm
# pouwer-dev-worker2          Ready    <none>          Xm

# Verify context is set
kubectl config current-context
# Expected: kind-pouwer-dev
```

### 1.4 Local Storage Prep (for PVCs)

kind does not have dynamic provisioning by default. Install the local-path provisioner:

```bash
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/v0.0.28/deploy/local-path-storage.yaml

# Set it as the default StorageClass
kubectl patch storageclass local-path \
  -p '{"metadata": {"annotations": {"storageclass.kubernetes.io/is-default-class": "true"}}}'

# Verify
kubectl get storageclass
```

---

## 2. Helm Installation & Setup

### 2.1 Install Helm

```bash
# macOS
brew install helm

# Linux
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Verify
helm version
# Expected: version.BuildInfo{Version:"v3.x.x", ...}
```

### 2.2 Add All Required Helm Repositories

These repos cover every chart needed for the full stack:

```bash
# Traefik — ingress controller
helm repo add traefik https://helm.traefik.io/traefik

# CrowdSec — security engine
helm repo add crowdsec https://crowdsecurity.github.io/helm-charts

# Cert-Manager — TLS (needed for Traefik HTTPS)
helm repo add jetstack https://charts.jetstack.io

# Metrics Server — optional but useful for HPA / kubectl top
helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/

# Bitnami — general-purpose charts (Monero node fallback, etc.)
helm repo add bitnami https://charts.bitnami.com/bitnami

# Update all repos
helm repo update

# Verify all repos are listed
helm repo list
```

---

## 3. Namespace & RBAC Setup

All workloads live in a dedicated namespace. RBAC is scoped tightly.

### 3.1 Create Namespace

```bash
kubectl create namespace pouwer-system

# Set as your default namespace for this work
kubectl config set-context --current --namespace=pouwer-system
# Or with kubens:
kubens pouwer-system
```

### 3.2 RBAC — CrowdSec Agent

CrowdSec's agent (DaemonSet) needs to read pod logs from the node. This requires a ClusterRole.

```yaml
# rbac/crowdsec-agent.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: crowdsec-agent
  namespace: pouwer-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: crowdsec-agent
rules:
  - apiGroups: [""]
    resources: ["pods", "nodes", "namespaces"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["pods/log"]
    verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: crowdsec-agent
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: crowdsec-agent
subjects:
  - kind: ServiceAccount
    name: crowdsec-agent
    namespace: pouwer-system
```

```bash
kubectl apply -f rbac/crowdsec-agent.yaml
```

### 3.3 RBAC — Traefik Ingress Controller

Traefik needs to watch Ingress, IngressRoute, and Service resources across all namespaces.

```yaml
# rbac/traefik.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: traefik
  namespace: pouwer-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: traefik-ingress-controller
rules:
  - apiGroups: [""]
    resources: ["services", "endpoints", "secrets"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["extensions", "networking.k8s.io"]
    resources: ["ingresses", "ingressclasses"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["extensions", "networking.k8s.io"]
    resources: ["ingresses/status"]
    verbs: ["update"]
  - apiGroups: ["traefik.io"]
    resources: ["ingressroutes", "ingressroutetcps", "ingressrouteudps",
                "middlewares", "middlewaretcps", "tlsoptions", "tlsstores",
                "serverstransports", "traefikservices"]
    verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: traefik-ingress-controller
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: traefik-ingress-controller
subjects:
  - kind: ServiceAccount
    name: traefik
    namespace: pouwer-system
```

```bash
kubectl apply -f rbac/traefik.yaml
```

---

## 4. Secrets

Sensitive values are pre-created as Kubernetes Secrets **before** any Helm install, so charts can reference them.

```bash
# CrowdSec bouncer API key (shared between Traefik plugin and CrowdSec LAPI)
kubectl create secret generic crowdsec-bouncer-secret \
  --namespace pouwer-system \
  --from-literal=bouncer-key="REPLACE_WITH_STRONG_KEY"

# Pouwer API credentials (if applicable)
kubectl create secret generic pouwer-api-secret \
  --namespace pouwer-system \
  --from-literal=api-key="REPLACE_WITH_POUWER_KEY"

# Verify secrets exist (values are not shown)
kubectl get secrets -n pouwer-system
```

> **Note**: For production, replace this with a proper secrets manager (Vault, Sealed Secrets, or External Secrets Operator). For local dev, `kubectl create secret` is fine.

---

## 5. CRD Installations

Some CRDs must be installed *before* the Helm charts that depend on them.

### 5.1 Traefik CRDs

The Traefik Helm chart installs its own CRDs automatically on first install. However, if you need to pre-install or upgrade them independently:

```bash
# Check if Traefik CRDs are already present
kubectl get crd | grep traefik

# If needed, install manually (from the traefik chart source)
helm show crds traefik/traefik | kubectl apply -f -
```

Key CRDs that will be present after Traefik install:
- `ingressroutes.traefik.io`
- `middlewares.traefik.io`
- `tlsoptions.traefik.io`
- `serverstransports.traefik.io`
- `traefikservices.traefik.io`

### 5.2 Cert-Manager CRDs (if using HTTPS)

```bash
# Install CRDs separately before the chart (recommended for cert-manager)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.15.0/cert-manager.crds.yaml

# Verify
kubectl get crd | grep cert-manager
```

---

## 6. Helm Chart Installations

Install in dependency order: cert-manager → Traefik → CrowdSec.

### 6.1 Cert-Manager (TLS prerequisite)

```bash
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.15.0 \
  --set installCRDs=false  # Already applied in 5.2

# Wait for it to be ready
kubectl rollout status deployment/cert-manager -n cert-manager
kubectl rollout status deployment/cert-manager-webhook -n cert-manager
```

### 6.2 Traefik

Create a values file to configure the plugin and access logs:

```yaml
# helm/traefik-values.yaml
deployment:
  replicas: 1

service:
  type: NodePort  # Use NodePort for kind; LoadBalancer requires MetalLB
  nodePorts:
    http: 30080
    https: 30443

ports:
  web:
    nodePort: 30080
  websecure:
    nodePort: 30443

ingressRoute:
  dashboard:
    enabled: true
    entryPoints: ["traefik"]

# CrowdSec Bouncer Plugin
experimental:
  plugins:
    bouncer:
      moduleName: "github.com/maxlerebourg/crowdsec-bouncer-traefik-plugin"
      version: "v1.3.5"

additionalArguments:
  - "--experimental.plugins.bouncer.moduleName=github.com/maxlerebourg/crowdsec-bouncer-traefik-plugin"
  - "--experimental.plugins.bouncer.version=v1.3.5"

# Access logs (CrowdSec agent reads these)
logs:
  access:
    enabled: true
    format: json

# RBAC
rbac:
  enabled: true

serviceAccount:
  name: traefik

# Persist plugin cache
persistence:
  enabled: true
  storageClass: local-path
  size: 128Mi
```

```bash
helm install traefik traefik/traefik \
  --namespace pouwer-system \
  --values helm/traefik-values.yaml \
  --version 28.x  # Pin to latest stable

# Verify rollout
kubectl rollout status deployment/traefik -n pouwer-system

# Verify CRDs were installed
kubectl get crd | grep traefik
```

### 6.3 CrowdSec

```yaml
# helm/crowdsec-values.yaml
container_runtime: containerd

agent:
  # DaemonSet reads logs from node paths
  acquisition:
    - namespace: pouwer-system
      podName: traefik-*
      program: traefik

  env:
    - name: COLLECTIONS
      value: "crowdsecurity/traefik crowdsecurity/http-cve crowdsecurity/linux"

lapi:
  env:
    - name: ENROLL_KEY
      value: ""  # Set if enrolling in CrowdSec console
    - name: ENROLL_INSTANCE_NAME
      value: "pouwer-dev-local"

  # Persist CrowdSec database
  persistentVolume:
    data:
      enabled: true
      storageClassName: local-path
      size: 1Gi
    config:
      enabled: true
      storageClassName: local-path
      size: 100Mi

  # Service account created in Section 3
  serviceAccount:
    create: false
    name: crowdsec-agent
```

```bash
helm install crowdsec crowdsec/crowdsec \
  --namespace pouwer-system \
  --values helm/crowdsec-values.yaml

# Verify LAPI pod is running
kubectl get pods -n pouwer-system -l app=crowdsec

# Watch agent DaemonSet come up
kubectl get daemonset -n pouwer-system
```

### 6.4 Register the Bouncer with CrowdSec LAPI

After CrowdSec LAPI is running, register the Traefik bouncer key:

```bash
# Exec into the LAPI pod
LAPI_POD=$(kubectl get pod -n pouwer-system -l app=crowdsec,component=lapi -o jsonpath='{.items[0].metadata.name}')

# Add a bouncer and retrieve the key (or use the key you pre-set in the Secret)
kubectl exec -n pouwer-system $LAPI_POD -- \
  cscli bouncers add traefik-bouncer

# If using the pre-created secret key, register it directly:
kubectl exec -n pouwer-system $LAPI_POD -- \
  cscli bouncers add traefik-bouncer -k "$(kubectl get secret crowdsec-bouncer-secret -n pouwer-system -o jsonpath='{.data.bouncer-key}' | base64 -d)"
```

---

## 7. Traefik–CrowdSec Middleware CRD

With both charts running and the bouncer key registered, create the Middleware that all Ingress/IngressRoute resources will reference:

```yaml
# crds/crowdsec-middleware.yaml
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: crowdsec-bouncer
  namespace: pouwer-system
spec:
  plugin:
    bouncer:
      enabled: true
      logLevel: INFO
      updateIntervalSeconds: 60
      defaultDecisionSeconds: 60
      httpTimeoutSeconds: 10
      crowdsecMode: stream
      crowdsecLapiKey: ""  # Injected via env; see note below
      crowdsecLapiKeyFile: ""
      crowdsecLapiHost: "crowdsec-service.pouwer-system.svc.cluster.local:8080"
      crowdsecLapiScheme: http
      forwardedHeadersTrustedIPs:
        - "10.0.0.0/8"
```

```bash
kubectl apply -f crds/crowdsec-middleware.yaml

# Verify the middleware is recognized
kubectl describe middleware crowdsec-bouncer -n pouwer-system
```

---

## 8. Metrics Server (Optional but Recommended)

Enables `kubectl top nodes/pods` and is required for Horizontal Pod Autoscaler:

```bash
helm install metrics-server metrics-server/metrics-server \
  --namespace kube-system \
  --set args[0]="--kubelet-insecure-tls"  # Required for kind

# Verify
kubectl top nodes
```

---

## 9. Infrastructure Verification Checklist

Run these checks before handing off to the application deployment track.

```bash
# 1. All nodes Ready
kubectl get nodes

# 2. All infra pods Running (no CrashLoopBackOff)
kubectl get pods -n pouwer-system
kubectl get pods -n cert-manager

# 3. StorageClass available and default
kubectl get storageclass

# 4. Traefik CRDs present
kubectl get crd | grep traefik
# Should list: ingressroutes, middlewares, tlsoptions, etc.

# 5. Traefik accessible
curl -o /dev/null -s -w "%{http_code}" http://localhost:80
# Expected: 404 (no routes yet, but Traefik is answering)

# 6. CrowdSec LAPI healthy
LAPI_POD=$(kubectl get pod -n pouwer-system -l component=lapi -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n pouwer-system $LAPI_POD -- cscli version

# 7. Bouncer registered
kubectl exec -n pouwer-system $LAPI_POD -- cscli bouncers list

# 8. Secrets exist
kubectl get secrets -n pouwer-system

# 9. RBAC ClusterRoleBindings present
kubectl get clusterrolebindings | grep -E "traefik|crowdsec"

# 10. Middleware CRD applied
kubectl get middlewares -n pouwer-system
```

---

## 10. Cluster Teardown & Reset

For iterative local testing:

```bash
# Full cluster teardown
kind delete cluster --name pouwer-dev

# Recreate from scratch
kind create cluster --config kind-cluster.yaml
```

---

## Appendix: File Structure

The recommended layout for all infra-related files before application charts are added:

```
k8s/
├── kind-cluster.yaml           # Cluster definition
├── rbac/
│   ├── crowdsec-agent.yaml
│   └── traefik.yaml
├── crds/
│   └── crowdsec-middleware.yaml
└── helm/
    ├── traefik-values.yaml
    └── crowdsec-values.yaml
```
