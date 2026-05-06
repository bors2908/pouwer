import {BaseTask, ISolver, JobType, Progress, SolveResult, WorkerOutMessage} from "./models";

type WorkerScriptMap = Record<JobType, new () => Worker>;

interface WorkerEntry {
    worker: Worker;
    reject: (err: Error) => void;
}

export class WebWorkerSolver<TTask extends BaseTask = BaseTask, TResultPayload = unknown> implements ISolver<TTask, TResultPayload> {
    private workers = new Map<JobType, WorkerEntry>();

    private readonly scriptMap: WorkerScriptMap;

    constructor(scriptMap: WorkerScriptMap) {
        this.scriptMap = scriptMap;
    }

    async start(task: TTask, onProgress?: (stats: Progress) => void): Promise<SolveResult<TResultPayload>> {
        const jobType = task.jobType;
        const WorkerConstructor = this.scriptMap[jobType];

        if (!WorkerConstructor) {
            throw new Error(`No worker script registered for jobType: ${String(jobType)}`);
        }

        if (this.workers.has(jobType)) {
            throw new Error(`Worker for jobType ${String(jobType)} is already running`);
        }

        return new Promise<SolveResult<TResultPayload>>((resolve, reject) => {
            const worker = new WorkerConstructor();

            const entry: WorkerEntry = {worker, reject};
            this.workers.set(jobType, entry);

            const cleanup = () => {
                try {
                    entry.worker.onmessage = null;
                    entry.worker.onerror = null;
                } finally {
                    if (this.workers.get(jobType) === entry) {
                        this.workers.delete(jobType);
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

    cancel(jobType?: JobType): void {
        if (jobType !== undefined) {
            const entry = this.workers.get(jobType);
            if (!entry) {
                return;
            }

            entry.worker.postMessage({type: "cancel"});
            entry.worker.terminate();
            this.workers.delete(jobType);
            entry.reject(new Error("Cancelled"));
            return;
        }

        for (const [activeJobType, entry] of Array.from(this.workers.entries())) {
            entry.worker.postMessage({type: "cancel"});
            entry.worker.terminate();
            entry.reject(new Error("Cancelled"));
            this.workers.delete(activeJobType);
        }
    }

    isRunning(jobType: JobType): boolean {
        return this.workers.has(jobType);
    }
}
