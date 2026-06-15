$props = @{}

$gradleProps = Join-Path $HOME ".gradle\gradle.properties"
if (Test-Path $gradleProps) {
    Get-Content $gradleProps | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+?)\s*=\s*(.*)\s*$') {
            $props[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
}

if (-not $props.ContainsKey("nexusUser") -or -not $props.ContainsKey("nexusPass")) {
    throw "nexusUser or nexusPass not found in $gradleProps"
}

kind create cluster --config examples/crowdsec-traefik-integration/k8s/kind-cluster.yaml --name pouwer-dev

kubectl create namespace crowdsec
kubectl create namespace traefik
kubectl create namespace pouwer-system

kubectl create secret docker-registry nexus-creds `
  --docker-server=172.21.0.1:9002 `
  --docker-username=$($props["nexusUser"]) `
  --docker-password=$($props["nexusPass"]) `
  -n pouwer-system

kubectl create secret docker-registry nexus-creds `
  --docker-server=172.21.0.1:9002 `
  --docker-username=$($props["nexusUser"]) `
  --docker-password=$($props["nexusPass"]) `
  -n traefik

kubectl apply -f examples/crowdsec-traefik-integration/k8s/rbac/crowdsec-agent.yaml
kubectl apply -f examples/crowdsec-traefik-integration/k8s/rbac/traefik.yaml -n traefik

kubectl apply -f examples/crowdsec-traefik-integration/k8s/storage/pouwer-pages-pvc.yaml -n traefik

helm repo add traefik https://traefik.github.io/charts
helm repo add crowdsec https://crowdsec-project.github.io/helm-charts
helm repo update

helm install traefik traefik/traefik -n traefik --values examples/crowdsec-traefik-integration/k8s/helm/traefik-values.yaml
helm install crowdsec crowdsec/crowdsec -n crowdsec --values examples/crowdsec-traefik-integration/k8s/helm/crowdsec-values.yaml
helm install pouwer-ingress ./examples/crowdsec-traefik-integration/k8s/helm/ingress -n pouwer-system

kubectl apply -f examples/crowdsec-traefik-integration/k8s/crds/crowdsec-middleware.yaml

kubectl apply -f examples/crowdsec-traefik-integration/k8s/infra/monero-node.yaml -n pouwer-system
kubectl apply -f examples/crowdsec-traefik-integration/k8s/infra/p2pool.yaml -n pouwer-system

kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/traefik.yaml -n traefik
kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/pouwer-stack.yaml -n pouwer-system
kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/example-apps.yaml -n pouwer-system
