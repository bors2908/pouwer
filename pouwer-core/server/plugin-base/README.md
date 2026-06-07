# pouwer-core:server:plugin-base

Shared Spring Boot base for Pouwer plugin services.

Use this module when writing a JVM plugin. It exposes the standard `/plugin/*` HTTP contract, registers the plugin in core after startup, and sends periodic heartbeats.

## Provided Pieces

| Class | Purpose |
| --- | --- |
| `PluginPayloadService` | Interface your plugin implements: `pluginId`, `createTask`, `validate`, optional `supports`. |
| `BasePluginController` | Implements `/payload/build`, `/payload/validate`, `/supports`, and `/health` under your chosen controller mapping. |
| `BasePluginRegistration` | Registers the plugin with core on `ApplicationReadyEvent` and sends heartbeat on a schedule. |
| `PluginBaseConfiguration` | Enables scheduling, binds properties, and provides `RestTemplate`. |
| `PluginBaseProperties` | Binds `plugin.core.*` configuration. |
| `PluginValidationRequest` | Shared request wrapper for plugin validation: stored `Task` plus submitted `ResultMessage`. |

## HTTP Contract

If your controller maps `BasePluginController` under `/plugin`, it exposes:

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/plugin/payload/build` | Receives `PayloadBuildRequest`, returns `Task`. |
| `POST` | `/plugin/payload/validate` | Receives `PluginValidationRequest`, returns `ValidationResult`. |
| `POST` | `/plugin/supports` | Receives `PayloadSupportContext`, returns boolean. |
| `GET` | `/plugin/health` | Returns `status=UP` and the plugin ID. |

Registration endpoints called on core:

| Method | Path |
| --- | --- |
| `POST` | `{plugin.core.url}/core/plugins/register` |
| `POST` | `{plugin.core.url}/core/plugins/{pluginId}/heartbeat` |

## Build

From repository root:

```bash
./gradlew :pouwer-core:server:plugin-base:build
./gradlew :pouwer-core:server:plugin-base:test
```

Windows:

```powershell
.\gradlew.bat :pouwer-core:server:plugin-base:build
.\gradlew.bat :pouwer-core:server:plugin-base:test
```

`bootJar` is disabled because this is a library module, not a runnable application.

## Building A New JVM Plugin

1. Create a new Gradle module that applies `java`, Kotlin JVM/Spring, Spring Boot, and usually Jib.
2. Depend on `:pouwer-core:server:plugin-base`.
3. Implement `PluginPayloadService`.
4. Keep your payload models in the plugin module. Convert them to/from `JsonNode` inside the plugin.
5. Add a controller:

```kotlin
@RestController
@RequestMapping("/plugin")
class MyPluginController(payloadPlugin: MyPayloadPlugin) : BasePluginController(payloadPlugin)
```

6. Add a registration component extending `BasePluginRegistration` and return the plugin ID, port, and externally reachable host.
7. Import `classpath:application-base.yml` from your plugin `application.yml`.
8. Provide a matching browser worker/bundle if the plugin is meant to be solved by the browser widget.

## Required Runtime Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `plugin.core.url` | `http://localhost:8082` | Core runtime URL used for registration and heartbeat. |
| `plugin.core.heartbeat.interval` | `30000` | Heartbeat interval in milliseconds. |
| `plugin.host` | `localhost` | Hostname or base host core should call back. Existing plugins use values like `http://pouwer-monero-plugin` in Docker. |
| `server.port` | plugin-specific | Port included in the registered base URL. |

## Technical Notes

- `BasePluginRegistration.getBaseUrl()` builds `${plugin.host}:${server.port}`. In Docker, `plugin.host` should include the scheme and service DNS name, for example `http://pouwer-sha256-plugin`.
- `PluginPayloadService.supports()` defaults to exact `requestedPluginId` match when one is provided; override it only when a plugin intentionally supports aliases or richer routing.
- A plugin should return `CONFLICT` for lease/job-state problems such as nonce ranges or missing stored templates, and `REJECTED` for bad work.
- Registration failure currently fails startup by throwing from the application-ready listener. Start core before starting plugins.
