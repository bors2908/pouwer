# Pouwer K8s Cluster Management Guide

This guide provides commands to manage the local `kind` cluster used for the Pouwer-CrowdSec-Traefik integration.

## 1. Prerequisites

- `kind` installed.
- `kubectl` installed.
- `helm` installed.
- Docker running.
- Access to the Nexus registry at `172.21.0.1:9001/9002` (if deploying application components).

## 2. Start Cluster Cleanly (None to Running)

Follow these steps to set up the environment from scratch.

### 2.1 Create the Cluster
```powershell
kind create cluster --config examples/crowdsec-traefik-integration/k8s/kind-cluster.yaml --name pouwer-dev
```

### 2.2 Create Namespaces
```powershell
kubectl create namespace crowdsec
kubectl create namespace traefik
kubectl create namespace pouwer-system
```

### 2.3 Setup Registry Credentials (Optional)
If you need to pull private images from Nexus:
```powershell
# Replace with your actual credentials
kubectl create secret docker-registry nexus-creds --docker-server=172.21.0.1:9002 --docker-username=<USERNAME> --docker-password=<PASSWORD> -n pouwer-system
```

### 2.4 Deploy Infrastructure Layer
```powershell
# RBAC
kubectl apply -f examples/crowdsec-traefik-integration/k8s/rbac/crowdsec-agent.yaml
kubectl apply -f examples/crowdsec-traefik-integration/k8s/rbac/traefik.yaml -n traefik

# Storage
kubectl apply -f examples/crowdsec-traefik-integration/k8s/storage/pouwer-pages-pvc.yaml -n traefik
kubectl apply -f examples/crowdsec-traefik-integration/k8s/storage/pouwer-pages-pvc.yaml -n pouwer-system

# Helm Charts
helm repo add traefik https://traefik.github.io/charts
helm repo add crowdsec https://crowdsec-project.github.io/helm-charts
helm repo update

helm install traefik traefik/traefik -n traefik --values examples/crowdsec-traefik-integration/k8s/helm/traefik-values.yaml
helm install crowdsec crowdsec/crowdsec -n crowdsec --values examples/crowdsec-traefik-integration/k8s/helm/crowdsec-values.yaml

# Middleware & CRDs
kubectl apply -f examples/crowdsec-traefik-integration/k8s/crds/crowdsec-middleware.yaml

# Monero & P2Pool
kubectl apply -f examples/crowdsec-traefik-integration/k8s/infra/monero-node.yaml -n pouwer-system
kubectl apply -f examples/crowdsec-traefik-integration/k8s/infra/p2pool.yaml -n pouwer-system
```

### 2.5 Build and Push Example Apps
Before deploying, build and push the example apps to the Nexus registry:
```powershell
./examples/crowdsec-traefik-integration/example-apps/push-images.ps1
```

### 2.6 Deploy Application Layer
```powershell
kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/pouwer-stack.yaml -n pouwer-system
kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/example-apps.yaml -n pouwer-system
kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/ingress.yaml -n pouwer-system
```

---

## 3. Stop and Delete Cluster

To completely remove the environment:

### 3.1 Delete Cluster
```powershell
kind delete cluster --name pouwer-dev
```

### 3.2 Cleanup Local Data (Optional)
If you used hostPath mounts:
```powershell
Remove-Item -Recurse -Force /tmp/pouwer-data
```

---

## 4. Verification Commands

Check if everything is running correctly:
```powershell
# Check all pods
kubectl get pods -A

# Check services
kubectl get svc -A

# Check logs for a specific component
kubectl logs -n pouwer-system -l app=pouwer-server
```
