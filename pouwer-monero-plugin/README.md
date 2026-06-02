# pouwer-monero-plugin

Monero RandomX Proof-of-Useful-Work plugin.

The plugin connects to a Stratum endpoint, keeps the latest mining job, converts it into a browser RandomX task, and validates browser shares by checking the target locally and submitting the share back to the pool.

## Modules

| Path | Description |
| --- | --- |
| `server` | Spring Boot plugin service. Plugin ID: `monero-randomx`. Default port: `8083`. |
| `browser/worker-monero` | RandomX worker package built on `randomx.js-shared`. |
| `browser/monero` | Traefik challenge bundle that boots `@pouwer/widget-traefik` with the Monero binding. |

## Build And Test

From repository root:

```bash
npm --workspace @pouwer/worker-monero run build
npm --workspace @pouwer/bundle-monero run build
./gradlew :pouwer-monero-plugin:server:build
./gradlew :pouwer-monero-plugin:server:test
```

Windows:

```powershell
npm --workspace @pouwer/worker-monero run build
npm --workspace @pouwer/bundle-monero run build
.\gradlew.bat :pouwer-monero-plugin:server:build
.\gradlew.bat :pouwer-monero-plugin:server:test
```

Build the Docker image:

```bash
./gradlew :pouwer-monero-plugin:server:jibDockerBuild
```

## Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8083` | Plugin HTTP port. |
| `stratum.host` | `127.0.0.1` | Stratum host. Compose overrides this to `p2pool`. |
| `stratum.port` | `3333` | Stratum port. |
| `stratum.worker` | `poctest.worker1` | Login/worker name. |
| `stratum.password` | empty | Stratum password. |
| `plugin.core.url` | `http://localhost:8082` | Core runtime URL inherited from `application-base.yml`. |
| `plugin.host` | `localhost` | Host registered with core. Override in Docker. |

## Running With Examples

The full integration compose already includes Monero testnet + P2Pool, core, and the Monero plugin:

```bash
cd examples/crowdsec-traefik-integration
docker compose up -d
```

Standalone Monero/P2Pool examples:

```bash
cd examples/third-party/docker/monero-testnet
docker compose up -d
```

```bash
cd examples/third-party/docker/monero-mainnet
docker compose up -d
```

Both standalone files use `D:/docker/...` bind mounts and should be adjusted outside that local Windows layout.

## Technical Notes

- `MoneroStratumTcpClient` connects during bean initialization, logs in, listens for `job` notifications, and updates `StratumJobStore`.
- `createTask()` fails if no Stratum job has arrived yet. Start the pool and wait for a job before expecting `/challenge` to work.
- `StratumToMoneroConverter` keeps Stratum fields in the browser payload: blob, target, height, job ID, and seed hash.
- Validation checks nonce range shape, parses the RandomX hash, compares the little-endian numeric hash against the target, then submits the share through Stratum.
- The browser worker uses `randomx.js-shared/web`. This is why the static proxy adds COOP/COEP headers and why the worker remains off the main thread.
- The bundle inlines `monero-worker.js` into `challenge-monero.js` for simpler static delivery through plugin resources.
