# pouwer-core:server:core-runtime

Spring Boot service that orchestrates Pouwer challenges.

The runtime is the central control plane: it accepts browser challenge requests, asks plugins to build payloads, stores task leases, validates browser results through plugins, manages plugin registration/heartbeat state, and proxies plugin static assets.

## Public Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/challenge?pluginId=...&workerId=...` | Selects the requested plugin, calls its build endpoint, stores the task, and returns it. |
| `POST` | `/validate` | Validates a JSON `ResultMessage`; returns `accepted`, `rejected`, or `conflict`. |
| `POST` | `/validate-custom-captcha` | Form-compatible validation endpoint for Traefik; reads field `response`. |
| `POST` | `/core/plugins/register` | Registers or re-registers a plugin service. |
| `POST` | `/core/plugins/{pluginId}/heartbeat` | Marks a plugin healthy and updates heartbeat timestamp. |
| `DELETE` | `/core/plugins/{pluginId}` | Removes a plugin registration. |
| `GET` | `/static/{pluginId}/...` | Proxies static files from a plugin service. |

## Build And Run

From repository root:

```bash
npm install
npm run build
./gradlew :pouwer-core:server:core-runtime:build
./gradlew :pouwer-core:server:core-runtime:test
./gradlew :pouwer-core:server:core-runtime:bootRun
```

Windows:

```powershell
npm install
npm run build
.\gradlew.bat :pouwer-core:server:core-runtime:build
.\gradlew.bat :pouwer-core:server:core-runtime:test
.\gradlew.bat :pouwer-core:server:core-runtime:bootRun
```

Build the Docker image:

```bash
./gradlew :pouwer-core:server:core-runtime:jibDockerBuild
```

Default port: `8082`.

## Configuration

Important defaults from `application.yml`:

| Property | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8082` | HTTP port. |
| `challenge.task.ttl-ms` | `60000` | Task lifetime in milliseconds. |
| `challenge.plugins.priority-override` | `monero-randomx,pow-test-sha256,bitcoin-rpc-sha256` | Preferred order for resolving static assets when no plugin is explicit. |
| `pages.extract.path` | empty | Optional page extraction path used by the integration stack. |

The plugin transport circuit breaker is configured through `resilience4j.circuitbreaker.instances.pluginTransport`.

## Technical Notes

- Task storage is in-memory (`InMemoryTaskStore`). Restarting core loses outstanding challenges.
- Plugin discovery is remote and heartbeat-based. `RemotePluginRegistry` stores registered plugins, marks failed plugins unhealthy, and evicts stale entries.
- Core adapts remote plugins to the local `PayloadPlugin` interface through `RemotePayloadPluginAdapter`. This keeps the validation pipeline independent of transport details.
- Plugin calls use `RestPluginTransport` behind `CircuitBreakerPluginTransport`. Transport failures mark the plugin unhealthy.
- Static proxying strips hop-by-hop headers and adds `Cross-Origin-Opener-Policy: same-origin` and `Cross-Origin-Embedder-Policy: require-corp`. Those headers matter for browser worker/WASM isolation.
- `POST /validate-custom-captcha` always returns HTTP 200 with `{ "success": true|false }` because CrowdSec/Traefik captcha integrations expect that shape.
