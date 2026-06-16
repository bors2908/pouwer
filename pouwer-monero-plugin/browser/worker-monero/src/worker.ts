// @ts-ignore
import {mine} from "randomx.js-shared/web";
import type {Config, Job, MinerCallbacks} from "randomx.js-shared";
import type {
    WorkerEventJobDisposed,
    WorkerEventJobStarted,
    WorkerEventNonceSpaceExhausted,
    WorkerEventResultFound,
    WorkerEventWorkerReady,
    WorkerPong
} from "randomx.js-shared";
import {createWorkerRuntime, nowMs, type WorkerRuntimeApi} from "@pouwer/worker-runtime";
import {RandomXResultPayload, RandomXTask} from "./types.js";

createWorkerRuntime<RandomXTask, RandomXResultPayload>(solve);

async function solve(task: RandomXTask, runtime: WorkerRuntimeApi<RandomXResultPayload>) {
    return solveRandomX(task, runtime);
}

async function solveRandomX(task: RandomXTask, runtime: WorkerRuntimeApi<RandomXResultPayload>) {
    const job: Job = {
        blob: task.payload.blob,
        job_id: task.jobId,
        target: task.payload.targetHex,
        height: task.payload.height,
        seed_hash: task.payload.seedHash,
    };
    const taskPayload = task.payload;
    const startTime = nowMs();
    const PROGRESS_INTERVAL_MS = 500;
    const minerStats = new Map<string, WorkerPong["stats"]>();
    let lastProgressTime = startTime;

    const callbacks: MinerCallbacks = {
        on_cache_initialising: () => {
            console.log("Cache Initialising...");
        },
        on_cache_initialised: (duration_ms: number) => {
            console.log(`Cache Initialised in ${duration_ms}ms`);
        },
        on_worker_ready: (_event: WorkerEventWorkerReady) => {
        },
        on_job_started: (_event: WorkerEventJobStarted) => {
        },
        on_job_disposed: (_event: WorkerEventJobDisposed) => {
        },
        on_nonce_space_exhausted: (_event: WorkerEventNonceSpaceExhausted) => {
            runtime.reportStopped("Exhausted");
        },
        on_result_found: (event: WorkerEventResultFound) => {
            if (!runtime.isRunning()) {
                return;
            }
            const durationMs = nowMs() - startTime;

            console.log(`Result found: ${toHex(event.result)}`);

            const result: RandomXResultPayload = {
                taskId: taskPayload.id,
                nonce: event.nonce,
                hash: toHex(event.result)
            };

            runtime.reportSolved({
                attempts: event.hash_count,
                durationMs: durationMs,
                payload: result,
            });
            return;
        },
        on_pong: (event: WorkerPong) => {
            if (!runtime.isRunning()) {
                return;
            }

            minerStats.set(event.miner_id, event.stats);

            const now = nowMs();
            if (now - lastProgressTime < PROGRESS_INTERVAL_MS) {
                return;
            }
            lastProgressTime = now;

            let hashesTotal = 0;
            let hashesPerSec = 0;
            for (const stats of minerStats.values()) {
                hashesTotal += stats.hashes_total;
                hashesPerSec += stats.hashes_per_second;
            }

            runtime.reportProgress({
                attempts: hashesTotal,
                elapsedMs: now - startTime,
                hashesPerSec: Math.floor(hashesPerSec),
            });
        },
    };

    const config: Config = {
        callbacks: callbacks,
    };

    mine(job, config);

    await new Promise(r => setTimeout(r, 200));
}

function toHex(u8: Uint8Array): string {
    return Array.from(u8).map(b => b.toString(16).padStart(2, "0")).join("");
}
