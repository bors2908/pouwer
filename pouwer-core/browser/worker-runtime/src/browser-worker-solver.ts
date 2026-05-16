import type {BaseTask, ISolver, Progress, SolveResult, WorkerOutMessage} from "@pouwer/core-contracts";

interface WorkerEntry {
    worker: Worker;
    reject: (err: Error) => void;
}

export class WebWorkerSolver<TTask extends BaseTask = BaseTask, TResultPayload = unknown> implements ISolver<TTask, TResultPayload> {
    private currentWorker: WorkerEntry | null = null;

    private readonly WorkerConstructor: new () => Worker;

    constructor(WorkerConstructor: new () => Worker) {
        this.WorkerConstructor = WorkerConstructor;
    }

    async start(task: TTask, onProgress?: (stats: Progress) => void): Promise<SolveResult<TResultPayload>> {
        if (this.currentWorker) {
            const pluginId = task.pluginId;
            throw new Error(`Worker for pluginId ${pluginId} is already running`);
        }

        return new Promise<SolveResult<TResultPayload>>((resolve, reject) => {
            const worker = new this.WorkerConstructor();

            const entry: WorkerEntry = {worker, reject};
            this.currentWorker = entry;

            const cleanup = () => {
                try {
                    entry.worker.onmessage = null;
                    entry.worker.onerror = null;
                } finally {
                    if (this.currentWorker === entry) {
                        this.currentWorker = null;
                    }
                }
            };

            entry.worker.onmessage = (event: MessageEvent<WorkerOutMessage<TResultPayload>>) => {
                const message = event.data;
                if (message.type === "progress") {
                    onProgress?.(message);
                    return;
                }
                if (message.type === "solved") {
                    cleanup();
                    entry.worker.terminate();
                    resolve(message);
                    return;
                }
                if (message.type === "stopped") {
                    cleanup();
                    entry.worker.terminate();
                    reject(new Error(message.reason || "Stopped"));
                }
            };

            entry.worker.onerror = (event) => {
                cleanup();
                entry.worker.terminate();
                reject(new Error(event.message || "Worker error"));
            };

            entry.worker.postMessage({type: "init", task});
            entry.worker.postMessage({type: "start"});
        });
    }

    cancel(_pluginId?: string): void {
        const entry = this.currentWorker;
        if (!entry) {
            return;
        }

        entry.worker.postMessage({type: "cancel"});
        entry.worker.terminate();
        this.currentWorker = null;
        entry.reject(new Error("Cancelled"));
    }

    isRunning(_pluginId: string): boolean {
        return this.currentWorker !== null;
    }
}
