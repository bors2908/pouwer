# @pouwer/core-contracts

Shared TypeScript protocol contracts for browser-side Pouwer code.

This package deliberately has no runtime dependencies. Widgets, worker runtime code, and plugin browser packages can all agree on task/result/message shapes without importing UI or solver implementations.

## What It Contains

- `BaseTask<TPayload>`: task returned by core, including `jobId`, `pluginId`, `expiresAt`, optional `leaseHmac`, and plugin-owned `payload`.
- `ResultMessage<TPayload>`: result sent back to core.
- `Progress` and `SolveResult`: generic progress/result shapes used by widgets.
- `WorkerInMessage` and `WorkerOutMessage`: worker protocol for `init`, `start`, `cancel`, `progress`, `solved`, and `stopped`.
- `PayloadBinding`: plugin ID plus worker factory binding used by widgets to select the right solver.

## Build

From this directory:

```bash
npm run build
npm run typecheck
```

From repository root:

```bash
npm --workspace @pouwer/core-contracts run build
```

There is no test script in this package.

## Technical Notes

- Payloads are generic by design. Core packages should not define SHA-256, Bitcoin, or Monero payload structure here unless that structure becomes part of the framework contract.
- The TypeScript shapes mirror the Kotlin contracts in `pouwer-core/server/core-api`, but they are maintained as source TypeScript rather than generated code.
- `jobId` is a string in the browser because JSON transports UUIDs as strings.
