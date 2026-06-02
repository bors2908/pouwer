# @pouwer/worker-runtime

Generic Web Worker runtime used by browser-side solvers.

The package has two halves: code used by the page to create and control a worker, and code used inside the worker to expose a small message-driven solving lifecycle.

## Runtime Model

- The page creates a `WebWorkerSolver` with a worker constructor.
- `start(task, onProgress)` creates exactly one worker, sends `init`, then sends `start`.
- The worker reports `progress`, `solved`, or `stopped`.
- On `solved`, `stopped`, or worker error, the page-side wrapper terminates the worker and clears its state.
- `cancel()` posts `cancel`, terminates the worker, and rejects the active promise.

## Build And Test

From this directory:

```bash
npm run build
npm run typecheck
npm run test
npm run test:coverage
```

From repository root:

```bash
npm --workspace @pouwer/worker-runtime run build
npm --workspace @pouwer/worker-runtime test
```

## Technical Notes

- Only one worker is active per `WebWorkerSolver` instance. This avoids ambiguous cancellation and progress routing.
- Workers are module workers. `createModuleWorkerFactory(url)` constructs workers with `{ type: "module" }`.
- `createWorkerRuntime()` is intentionally small: it tracks the current task, running state, and message conversion. Actual mining/search logic belongs in plugin worker packages.
- `cancel` is cooperative inside the worker loop. Solver code must periodically check `runtime.isRunning()` and yield, otherwise the page can terminate the worker but the worker code will not report a clean stop.
