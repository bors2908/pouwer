import {ISolver, JobType, Progress, SolveResult, Task, WorkerOutMessage} from "./types";

type WorkerScriptMap = Record<JobType, new () => Worker>;

interface WorkerEntry {
    worker: Worker;
    resolve: (res: SolveResult) => void;
    reject: (err: any) => void;
    onProgress?: (p: Progress) => void;
}

/**
 * WebWorkerSolver: one worker instance per JobType.
 * constructor accepts a map from JobType -> worker constructor (Vite worker import).
 */
export class WebWorkerSolver implements ISolver {
    private workers = new Map<JobType, WorkerEntry>();

    private readonly scriptMap: WorkerScriptMap;

    constructor(scriptMap: WorkerScriptMap) {
        this.scriptMap = scriptMap;
    }

    async start(task: Task, onProgress?: (stats: Progress) => void): Promise<SolveResult> {
        const jobType = task.jobType as JobType;
        const WorkerConstructor = this.scriptMap[jobType];

        if (!WorkerConstructor) {
            throw new Error(`No worker script registered for jobType: ${String(jobType)}`);
        }

        if (this.workers.has(jobType)) {
            throw new Error(`Worker for jobType ${String(jobType)} is already running`);
        }

        return new Promise<SolveResult>((resolve, reject) => {
            const worker = new WorkerConstructor();

            const entry: WorkerEntry = {worker, resolve, reject, onProgress};
            this.workers.set(jobType, entry);

            const cleanup = () => {
                // remove listeners and terminate if not already
                try {
                    entry.worker.onmessage = null;
                    entry.worker.onerror = null;
                } catch {
                }
                if (this.workers.get(jobType) === entry) {
                    this.workers.delete(jobType);
                }
            };

            entry.worker.onmessage = (e: MessageEvent<WorkerOutMessage>) => {
                const msg = e.data;
                if (msg.type === "progress") {
                    if (onProgress) {
                        onProgress(msg);
                    }
                    return;
                }
                if (msg.type === "solved") {
                    cleanup();
                    entry.worker.terminate();
                    resolve(msg as SolveResult);
                    return;
                }
                if (msg.type === "stopped") {
                    cleanup();
                    entry.worker.terminate();
                    reject(new Error(msg.reason || "Stopped"));
                    return;
                }
                // unknown message: ignore or log if you need
            };

            entry.worker.onerror = (e) => {
                cleanup();
                try {
                    entry.worker.terminate();
                } catch {
                }
                reject(e);
            };

            // initialize + start
            entry.worker.postMessage({type: "init", task: task});
            entry.worker.postMessage({type: "start"});
        });
    }

    /**
     * Cancel a specific worker by JobType, or all if no jobType is provided.
     */
    cancel(jobType?: JobType): void {
        if (jobType !== undefined) {
            const entry = this.workers.get(jobType);
            if (!entry) {
                return;
            }
            try {
                entry.worker.postMessage({type: "cancel"});
            } catch {
            }
            try {
                entry.worker.terminate();
            } catch {
            }
            this.workers.delete(jobType);
            // reject outstanding promise to signal cancellation (optional)
            entry.reject(new Error("Cancelled"));
            return;
        }

        // cancel all
        for (const [jt, entry] of Array.from(this.workers.entries())) {
            try {
                entry.worker.postMessage({type: "cancel"});
            } catch {
            }
            try {
                entry.worker.terminate();
            } catch {
            }
            entry.reject(new Error("Cancelled"));
            this.workers.delete(jt);
        }
    }

    /**
     * Utility: check whether a worker is active for given JobType
     */
    isRunning(jobType: JobType): boolean {
        return this.workers.has(jobType);
    }
}
