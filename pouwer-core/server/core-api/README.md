# pouwer-core:server:core-api

Shared Kotlin API contract between `core-runtime` and plugin services.

This module is the boundary that lets plugins remain separate deployable services. Core and plugins both depend on these classes, so changes here are contract changes.

## Main Contracts

| Type | Role |
| --- | --- |
| `Task` | Challenge issued to the browser. Contains `jobId`, `pluginId`, `expiresAt`, optional `leaseHmac`, and opaque `payload`. |
| `ResultMessage` | Browser result sent back to core. Contains `jobId`, optional `pluginId`, result `payload`, metadata, timing, and attempts. |
| `ValidationResult` | Plugin validation response with `ACCEPTED`, `REJECTED`, or `CONFLICT`. |
| `PayloadBuildRequest` | Core-to-plugin request for building a task. |
| `PayloadSupportContext` | Core-to-plugin request for checking whether a plugin supports a requested context. |
| `PayloadPlugin` | In-process abstraction used by core runtime adapters. |
| `PluginRegistration` | Plugin self-registration payload. |
| `PluginHeartbeat` | Plugin keepalive payload. |
| `PluginMetadata` | Core-side registered plugin record. |
| `PluginTransport` | Async transport abstraction used by core runtime. |

## Build

From repository root:

```bash
./gradlew :pouwer-core:server:core-api:build
./gradlew :pouwer-core:server:core-api:test
```

Windows:

```powershell
.\gradlew.bat :pouwer-core:server:core-api:build
.\gradlew.bat :pouwer-core:server:core-api:test
```

## Technical Notes

- `payload` is a Jackson `JsonNode`. This is the Opaque Payload Pattern used throughout the project: core transports payloads but does not parse plugin-specific data.
- `CHALLENGE_PLUGIN_CONTRACT_VERSION` is loaded from `contract.properties`. Plugins register with this version; core rejects mismatched plugin contracts when building its registry.
- `ResultMessage.pluginId` is optional for compatibility, but when present the validation pipeline checks it against the stored task.
- Keep this module small. Pulling runtime, transport implementation, or plugin-specific models into `core-api` would make independent plugins harder to maintain.
