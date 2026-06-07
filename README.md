# Pouwer

Pouwer is a Proof-of-Work / Proof-of-Useful-Work web protection framework. It sits behind an edge layer such as Traefik + CrowdSec, issues browser challenges, runs the solving work inside Web Workers, and validates submitted results through independently deployed payload plugins.

The useful part is the main design point. The core runtime does not know whether a payload is a toy SHA-256 challenge, Bitcoin block work, Monero RandomX mining, or something else. It stores and routes opaque task payloads; plugin services own payload construction, validation, and any external provider integration.

Runtime flow:

1. A challenge page loads a plugin-specific browser bundle.
2. The widget calls `GET /challenge?pluginId=...&workerId=...` on `pouwer-core:server:core-runtime`.
3. Core finds a healthy registered plugin and calls `POST /plugin/payload/build`.
4. Core stores the returned `Task` in an in-memory task store with a TTL.
5. The browser worker solves the task and returns a `ResultMessage`.
6. The widget submits the result to `POST /validate` or, for Traefik captcha pages, to `POST /validate-custom-captcha`.
7. Core checks job existence, expiry, plugin identity, and delegates result validation to `POST /plugin/payload/validate`.
8. The plugin returns `ACCEPTED`, `REJECTED`, or `CONFLICT`; core maps that to the public validation response.

## Repository Layout

| Path | Description |
| --- | --- |
| [pouwer-core](pouwer-core/README.md) | Shared Kotlin/TypeScript libraries plus the core runtime service. |
| [pouwer-sha256-plugin](pouwer-sha256-plugin/README.md) | Demonstration SHA-256 PoW plugin and browser bundle. |
| [pouwer-bitcoin-plugin](pouwer-bitcoin-plugin/README.md) | Bitcoin regtest-oriented SHA-256 PoUW plugin and browser bundle. |
| [pouwer-monero-plugin](pouwer-monero-plugin/README.md) | Monero RandomX Stratum PoUW plugin and browser bundle. |
| [examples/crowdsec-traefik-integration](examples/crowdsec-traefik-integration/) | Full edge integration stack with Traefik, CrowdSec, core, SHA-256 plugin, Monero plugin, and sample apps. |
| [examples/third-party/docker](examples/third-party/docker/) | External-service compose files for Bitcoin regtest and Monero/P2Pool. |
| [load-tests](load-tests/) | k6 scenarios, test orchestrator, and optional Prometheus/Grafana observability stack. |

## Prerequisites

- JDK 21. Gradle toolchains target Java 21.
- Node.js 20.x and npm 10.x. `gradle.properties` currently pins `node.version=20.19.5` and `npm.version=10.8.2` for Gradle-driven npm bundle tasks.
- Docker and Docker Compose for image builds and examples.
- k6, jq, and Bash 4+ for `load-tests/run-all.sh`.
- Optional local Maven/Docker registries if you keep the default repository properties:
  - Maven public: `http://localhost:9001/repository/maven-public/`
  - Docker hosted: `localhost:9002`

## Build And Test

Install browser dependencies and build every npm workspace:

```bash
npm install
npm run build
```

Build and test every Gradle module:

```bash
./gradlew build
./gradlew test
```

Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

Run the browser unit tests that currently exist:

```bash
npm --workspace @pouwer/worker-runtime test
```

Coverage for JVM modules is opt-in:

```bash
./gradlew test -PenableCoverage=true
```

Gradle server builds that use `ge.becrin.pouwer.npm-bundle` also run npm bundle work for the configured browser source directory and package the generated assets into Spring resources.

## Build Docker Images

The compose files expect images in the registry configured by `repo.url.docker.hosted`, defaulting to `localhost:9002`.

```bash
./gradlew :pouwer-core:server:core-runtime:jibDockerBuild
./gradlew :pouwer-sha256-plugin:server:jibDockerBuild
./gradlew :pouwer-monero-plugin:server:jibDockerBuild
./gradlew :pouwer-bitcoin-plugin:server:jibDockerBuild
```

Windows:

```powershell
.\gradlew.bat :pouwer-core:server:core-runtime:jibDockerBuild
.\gradlew.bat :pouwer-sha256-plugin:server:jibDockerBuild
.\gradlew.bat :pouwer-monero-plugin:server:jibDockerBuild
.\gradlew.bat :pouwer-bitcoin-plugin:server:jibDockerBuild
```

Jib image names:

| Module | Image | Exposed Port |
| --- | --- | --- |
| `:pouwer-core:server:core-runtime` | `localhost:9002/pouwer-server:0.2.2` | `8082` |
| `:pouwer-sha256-plugin:server` | `localhost:9002/pouwer-sha256-plugin:0.2.2` | `8085` |
| `:pouwer-monero-plugin:server` | `localhost:9002/pouwer-monero-plugin:0.2.2` | `8083` |
| `:pouwer-bitcoin-plugin:server` | `localhost:9002/pouwer-bitcoin-plugin:0.2.2` | `8084` |

## Docker Compose Examples

Full edge integration stack:

```bash
cd examples/crowdsec-traefik-integration
docker compose up -d
```

This stack starts:

- `pouwer-server` on `8082`
- `pouwer-monero-plugin` on `8083`
- `pouwer-sha256-plugin` on `8085`
- `traefik` on `80` and dashboard/API on `8081`
- `crowdsec`
- sample browser and API apps
- Monero testnet + P2Pool inside the same compose file

External Monero-only stacks:

```bash
cd examples/third-party/docker/monero-testnet
docker compose up -d
```

```bash
cd examples/third-party/docker/monero-mainnet
docker compose up -d
```

Bitcoin regtest stack:

```bash
cd examples/third-party/docker/bitcoin-regtest
docker compose up -d
```

## Load Tests

Start the integration stack first, then run the k6 orchestrator from `load-tests`:

```bash
cd load-tests
bash run-all.sh --target-host http://localhost:80 --core-host http://localhost:8082 --plugin-id pow-test-sha256
```

Useful variants:

```bash
bash run-all.sh --only T1,T2 --target-host http://localhost:80 --core-host http://localhost:8082
bash run-all.sh --skip T4 --plugin-id monero-randomx
bash run-all.sh --env-file envs/traefik-crowdsec-sha256.env
```

Scenarios:

| ID | Scenario |
| --- | --- |
| `T1` | Clean baseline through the target stack. |
| `T2` | Full PoUW challenge/validate cycle. |
| `T3` | Throughput ramp. |
| `T4` | Soak test, default `60m`, configurable with `SOAK_DURATION`. |
| `T5` | Configuration comparison wrapper. |
| `T6` | Direct `/challenge` load. |
| `T7` | Static asset load. |

Results are written to `load-tests/results/`.

Optional observability stack:

```bash
cd load-tests
docker compose -f docker-compose.observability.yml --profile observability up -d
```

Prometheus is exposed on `9090`, Grafana on `3000`, with Grafana admin password `admin`. The observability compose expects the Docker network `crowdsec-traefik-integration_app-net` to exist, so start the integration stack first.

## Public Runtime Endpoints

Core runtime:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/challenge?pluginId=...&workerId=...` | Builds and stores a task through the selected plugin. |
| `POST` | `/validate` | Validates a JSON `ResultMessage`. |
| `POST` | `/validate-custom-captcha` | Traefik-compatible form validation; field `response` contains serialized `ResultMessage`. |
| `POST` | `/core/plugins/register` | Plugin registration. |
| `POST` | `/core/plugins/{pluginId}/heartbeat` | Plugin keepalive. |
| `DELETE` | `/core/plugins/{pluginId}` | Plugin unregistration. |
| `GET` | `/static/{pluginId}/...` | Proxies static assets from a registered plugin. |

Plugin services:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/plugin/payload/build` | Build a plugin-owned task payload. |
| `POST` | `/plugin/payload/validate` | Validate a stored task plus browser result. |
| `POST` | `/plugin/supports` | Check whether the plugin can handle the request context. |
| `GET` | `/plugin/health` | Plugin health response. |

## Notes For Maintainers

- Payload JSON is intentionally opaque to core. Do not add plugin-specific payload parsing to `core-runtime`.
- - Plugins self-register on Spring `ApplicationReadyEvent` and send heartbeats every 30 seconds by default.
- `RemotePluginRegistry` evicts stale plugins after 120 seconds without heartbeat.
