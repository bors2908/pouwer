# pouwer-sha256-plugin

Demonstration SHA-256 Proof-of-Work plugin.

This plugin is the smallest complete payload implementation in the repository. It generates random 32-byte task data, assigns a nonce range, asks the browser to find a double-SHA-256 hash below a configured target, and validates the submitted nonce/hash pair.

## Modules

| Path | Description |
| --- | --- |
| `server` | Spring Boot plugin service. Plugin ID: `pow-test-sha256`. Default port: `8085`. |
| `browser/sha256` | Reusable SHA-256 worker package, also used by the Bitcoin browser bundle. |
| `browser/traefik-sha256` | Traefik challenge bundle that boots `@pouwer/widget-traefik` with the SHA-256 binding. |

## Build And Test

From repository root:

```bash
npm --workspace @pouwer/worker-sha256 run build
npm --workspace @pouwer/bundle-sha256 run build
./gradlew :pouwer-sha256-plugin:server:build
./gradlew :pouwer-sha256-plugin:server:test
```

Windows:

```powershell
npm --workspace @pouwer/worker-sha256 run build
npm --workspace @pouwer/bundle-sha256 run build
.\gradlew.bat :pouwer-sha256-plugin:server:build
.\gradlew.bat :pouwer-sha256-plugin:server:test
```

Build the Docker image:

```bash
./gradlew :pouwer-sha256-plugin:server:jibDockerBuild
```

## Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8085` | Plugin HTTP port. |
| `plugin.sha256.target` | `0000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff` | Target threshold for validation. |
| `plugin.sha256.nonce-range-size` | `1000000` | Number of nonces assigned per task. |
| `plugin.core.url` | `http://localhost:8082` | Core runtime URL inherited from `application-base.yml`. |
| `plugin.host` | `localhost` | Host registered with core. Override in Docker. |

## Technical Notes

- The task payload is intentionally compatible with Bitcoin-style SHA-256 work: `dataHex`, `nonceOffset`, `nonceIsLE`, `targetHex`, and `nonceRange`.
- The SHA-256 worker writes the nonce into the configured offset, double-hashes, reverses the hash for target comparison, and reports the reversed hex as `hashHex`.
- The server validates both the nonce lease and the submitted hash. A nonce outside the assigned range returns `CONFLICT`; a bad hash returns `REJECTED`.
- The Traefik bundle inlines the worker source into `challenge-sha256.js`. The fallback URL is `/assets/sha256-worker.js`, but the normal build removes that asset after embedding it.
- The server Gradle build bundles `browser/traefik-sha256/dist` into `static/sha256`, which core later serves through `/static/pow-test-sha256/...`.
