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

function Get-OpenSslPath {
    $cmd = Get-Command openssl -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $candidates = @(
        "C:\Program Files\Git\usr\bin\openssl.exe",
        "C:\Program Files (x86)\Git\usr\bin\openssl.exe"
    )
    foreach ($path in $candidates) {
        if (Test-Path $path) { return $path }
    }

    throw "openssl not found. Install Git for Windows or add openssl to PATH."
}

$openssl = Get-OpenSslPath

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

$certDir = "examples/crowdsec-traefik-integration/k8s/certs"
New-Item -ItemType Directory -Force -Path $certDir | Out-Null

$opensslConfig = @"
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
x509_extensions = san

[dn]
CN = pouwer-dev.local

[san]
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = example.com
DNS.3 = api.example.com
IP.1 = 127.0.0.1
IP.2 = 192.168.17.177
"@

$opensslConfigPath = Join-Path $certDir "openssl.cnf"
$tlsKeyPath = Join-Path $certDir "tls.key"
$tlsCertPath = Join-Path $certDir "tls.crt"
$opensslConfig | Set-Content -Path $opensslConfigPath -Encoding ascii

& $openssl req -x509 -nodes -days 3650 -newkey rsa:2048 `
  -keyout $tlsKeyPath `
  -out $tlsCertPath `
  -config $opensslConfigPath

kubectl create secret tls pouwer-dev-tls `
  --cert=$tlsCertPath `
  --key=$tlsKeyPath `
  -n traefik `
  --dry-run=client -o yaml | kubectl apply -f -

helm repo add traefik https://traefik.github.io/charts
helm repo add crowdsec https://crowdsec-project.github.io/helm-charts
helm repo update

helm install traefik traefik/traefik -n traefik --values examples/crowdsec-traefik-integration/k8s/helm/traefik-values.yaml
helm install crowdsec crowdsec/crowdsec -n crowdsec --values examples/crowdsec-traefik-integration/k8s/helm/crowdsec-values.yaml
helm install pouwer-ingress ./examples/crowdsec-traefik-integration/k8s/helm/ingress -n pouwer-system

kubectl apply -f examples/crowdsec-traefik-integration/k8s/crds/crowdsec-middleware.yaml

kubectl apply -f examples/crowdsec-traefik-integration/k8s/infra/monero-node.yaml -n pouwer-system
kubectl apply -f examples/crowdsec-traefik-integration/k8s/infra/p2pool.yaml -n pouwer-system

kubectl apply -f examples/crowdsec-traefik-integration/k8s/infra/redis.yaml -n pouwer-system
kubectl wait --for=condition=available deployment/redis -n pouwer-system --timeout=120s

kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/traefik.yaml -n traefik
kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/pouwer-stack.yaml -n pouwer-system
kubectl apply -f examples/crowdsec-traefik-integration/k8s/app/example-apps.yaml -n pouwer-system
