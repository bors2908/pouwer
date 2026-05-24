import type {BaseTask, SolveResult, WorkerInMessage, WorkerOutMessage, WorkerProgress} from "@pouwer/core-contracts";

export interface WorkerRuntimeApi<TResultPayload = unknown> {
    isRunning(): boolean;
    reportProgress(progress: Omit<WorkerProgress, "type">): void;
    reportSolved(result: SolveResult<TResultPayload>): void;
    reportStopped(reason: string): void;
}

type WorkerSolveHandler<TTask extends BaseTask, TResultPayload> = (
    task: TTask,
    runtime: WorkerRuntimeApi<TResultPayload>
) => Promise<void> | void;

function toErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
}

export function createWorkerRuntime<TTask extends BaseTask, TResultPayload>(
    solve: WorkerSolveHandler<TTask, TResultPayload>
) {
    let currentTask: TTask | null = null;
    let running = false;

    const runtime: WorkerRuntimeApi<TResultPayload> = {
        isRunning: () => running,
        reportProgress: (progress) => {
            if (!running) {
                return;
            }
            self.postMessage({
                type: "progress",
                ...progress,
            } as WorkerOutMessage<TResultPayload>);
        },
        reportSolved: (result) => {
            if (!running) {
                return;
            }
            running = false;
            self.postMessage({
                type: "solved",
                ...result,
            } as WorkerOutMessage<TResultPayload>);
        },
        reportStopped: (reason) => {
            if (!running) {
                return;
            }
            running = false;
            self.postMessage({
                type: "stopped",
                reason,
            } as WorkerOutMessage<TResultPayload>);
        },
    };

    self.onmessage = (event: MessageEvent<WorkerInMessage<TTask>>) => {
        const message = event.data;
        switch (message.type) {
            case "init":
                currentTask = message.task;
                break;
            case "start":
                if (running) {
                    return;
                }
                if (!currentTask) {
                    self.postMessage({type: "stopped", reason: "Task was not initialized"} as WorkerOutMessage<TResultPayload>);
                    return;
                }
                running = true;
                Promise.resolve()
                    .then(() => solve(currentTask, runtime))
                    .catch((error: unknown) => {
                        if (running) {
                            runtime.reportStopped(toErrorMessage(error));
                        }
                    });
                break;
            case "cancel":
                running = false;
                break;
        }
    };
}
