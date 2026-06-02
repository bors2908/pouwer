# @pouwer/widget-base

Generic browser challenge widget.

`widget-base` owns UI state, challenge fetching, worker-backed solving, progress rendering, and the final `ResultMessage` construction. It does not decide how a solved result is delivered to an edge integration; that is supplied through the `onSolved` callback.

## Expected Page Contract

The default UI code expects these element IDs:

| ID | Purpose |
| --- | --- |
| `captcha` | Root container. May define `data-challenge-url`. |
| `status` | Current phase/status text. |
| `hps` | Hashes-per-second display. |
| `attempts` | Attempt counter display. |
| `btnStart` | Start button. |
| `btnCancel` | Cancel button. |
| `result` | Result/error display. |

The challenge URL defaults to `/challenge`. If `data-challenge-url` is present on `#captcha`, that value is used instead.

## Build

From this directory:

```bash
npm run build
npm run typecheck
```

From repository root:

```bash
npm --workspace @pouwer/widget-base run build
```

There is no test script in this package.

## Technical Notes

- Browser `Worker` support is required. There is no main-thread fallback. That is intentional because PoW/PoUW work must not block the UI thread.
- `NetworkClient.getChallenge(pluginId)` sends both `pluginId` and a simple random `workerId`. Some plugins use `workerId` to assign a nonce subrange.
- The widget checks `expiresAt` after fetching a task, but core remains the authority during validation.
- `onSolved(resultMessage, ui)` is the integration hook. The base widget should stay ignorant of Traefik, forms, redirects, or other edge-specific delivery rules.
