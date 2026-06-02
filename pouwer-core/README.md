# pouwer-core

`pouwer-core` is the reusable core of the framework: shared server-side contracts/libraries, shared browser-side TypeScript packages, and the deployable core runtime service.

The important boundary is that core owns orchestration, task lifetime, plugin discovery, validation routing, and static proxying. It does not own plugin payload formats. Payloads are opaque JSON and are interpreted only by the plugin and its matching browser worker.

## Module Schema

| Module | Description |
| --- | --- |
| [browser/core-contracts](browser/core-contracts/README.md) | Shared TypeScript protocol types for tasks, results, progress, worker messages, and payload bindings. |
| [browser/worker-runtime](browser/worker-runtime/README.md) | Generic Web Worker lifecycle wrapper used by plugin workers. |
| [browser/widget-base](browser/widget-base/README.md) | Generic challenge widget: fetch task, start solver, show progress, emit result. |
| [browser/widget-traefik](browser/widget-traefik/README.md) | Traefik captcha-page adapter around `widget-base`. |
| `server/common` | Shared Spring/Jackson/security/test dependency surface used by server modules. |
| `server/common-test` | Test dependencies and utilities shared by JVM tests. |
| `server/model` | Low-level shared model module. |
| [server/core-api](server/core-api/README.md) | Kotlin API contracts shared by core runtime and plugins. |
| [server/core-runtime](server/core-runtime/README.md) | Spring Boot core runtime service. |
| [server/plugin-base](server/plugin-base/README.md) | Base Spring components for building plugin services. |

## Build

From repository root:

```bash
npm install
npm run build
./gradlew :pouwer-core:server:core-runtime:build
./gradlew :pouwer-core:server:core-runtime:test
```

Windows:

```powershell
npm install
npm run build
.\gradlew.bat :pouwer-core:server:core-runtime:build
.\gradlew.bat :pouwer-core:server:core-runtime:test
```

The core runtime Gradle build bundles `pouwer-core/browser` assets and copies Traefik pages from `browser/widget-traefik/pages` into the runtime resources.
