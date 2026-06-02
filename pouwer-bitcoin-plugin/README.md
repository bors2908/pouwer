# pouwer-bitcoin-plugin

Bitcoin regtest-oriented Proof-of-Useful-Work plugin.

The plugin turns a Bitcoin `getblocktemplate` response into browser-solvable SHA-256 header work. If the browser finds a valid nonce, the plugin reconstructs the block and submits it back to the node.

## Modules

| Path | Description |
| --- | --- |
| `server` | Spring Boot plugin service. Plugin ID: `bitcoin-rpc-sha256`. Default port: `8084`. |
| `browser/bitcoin` | Traefik challenge bundle using the shared SHA-256 worker with the Bitcoin plugin binding. |

The browser bundle depends on `@pouwer/worker-sha256` from `pouwer-sha256-plugin/browser/sha256`.

## Build And Test

From repository root:

```bash
npm --workspace @pouwer/worker-sha256 run build
npm --workspace @pouwer/bundle-bitcoin run build
./gradlew :pouwer-bitcoin-plugin:server:build
./gradlew :pouwer-bitcoin-plugin:server:test
```

Windows:

```powershell
npm --workspace @pouwer/worker-sha256 run build
npm --workspace @pouwer/bundle-bitcoin run build
.\gradlew.bat :pouwer-bitcoin-plugin:server:build
.\gradlew.bat :pouwer-bitcoin-plugin:server:test
```

Build the Docker image:

```bash
./gradlew :pouwer-bitcoin-plugin:server:jibDockerBuild
```

## Runtime Assumptions

Current `BitcoinRpcClient` values are hard-coded:

| Setting | Value |
| --- | --- |
| RPC URL | `http://localhost:18443` |
| RPC user | `rpcuser` |
| RPC password | `rpcpassword` |

That means the plugin currently expects a reachable local regtest node from the plugin process. In Docker, this needs code/configuration work or network/host mapping that makes `localhost:18443` correct inside the container.

The example compose at `examples/third-party/docker/bitcoin-regtest` declares `bitcoind` on `18443`, but this checkout does not contain the Dockerfile referenced by `build: .`.

## Technical Notes

- Work is based on Bitcoin block-header double-SHA-256. The browser uses the same SHA-256 worker as the demo plugin.
- `workerId` is converted to a numeric shard and multiplied by a hard-coded `chunkSize` of `500000` nonces.
- The plugin stores the original block template in an in-memory `BitcoinTemplateStore` keyed by `jobId` until expiry. If the template is gone during validation, the result is `CONFLICT`.
- Validation is two-stage: first verify nonce range and hash target locally, then reconstruct and submit the block through RPC.
- The block builder uses bitcoinj `RegTestParams`, creates a simple coinbase transaction, computes the merkle root, and serializes the reconstructed block.
- The browser bundle inlines `sha256-worker.js` into `challenge-bitcoin.js` for single-file static delivery through the plugin/core proxy path.
