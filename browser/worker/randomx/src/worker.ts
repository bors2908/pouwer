// @ts-ignore
import { mine } from "randomx.js-shared/web";
import type {Config, Job, MinerCallbacks} from "randomx.js-shared";
import type {
    WorkerEventJobDisposed,
    WorkerEventJobStarted,
    WorkerEventNonceSpaceExhausted,
    WorkerEventResultFound,
    WorkerEventWorkerReady,
    WorkerPong
} from "randomx.js-shared";

import {JobType} from "@pouwer/core-contracts";
import {createWorkerRuntime, nowMs, type WorkerRuntimeApi} from "@pouwer/worker-runtime";
import {RandomXResultPayload, RandomXTask} from "./types.js";

createWorkerRuntime<RandomXTask, RandomXResultPayload>(solve);

async function solve(task: RandomXTask, runtime: WorkerRuntimeApi<RandomXResultPayload>) {
    switch (task.jobType) {
        case JobType.MONERO_RANDOMX:
            return solveRandomX(task, runtime);
        default:
            runtime.reportStopped(`Unsupported job type: ${task.jobType}`);
    }
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

    const callbacks: MinerCallbacks = {
        on_cache_initialising: () => {
            console.log("Cache Initialising...");
        },
        on_cache_initialised: (duration_ms: number) => {
            console.log(`Cache Initialised in ${duration_ms}ms`);
        },
        on_worker_ready: (event: WorkerEventWorkerReady) => {
            console.log(`Worker ${event.miner_id} ready`);
        },
        on_job_started: (event: WorkerEventJobStarted) => {
            console.log(`Job ${event.job_id} started on worker ${event.miner_id}`);
        },
        on_job_disposed: (event: WorkerEventJobDisposed) => {
            console.log(`Job disposed on worker ${event.miner_id}`);
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
                hash: toHex(event.result),
                jobType: JobType.MONERO_RANDOMX,
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
            const elapsedMs = nowMs() - startTime;

            console.log(`Pong: ${event.stats.hashes_total} hashes in ${elapsedMs}ms`);

            runtime.reportProgress({
                attempts: event.stats.hashes_total,
                elapsedMs,
                hashesPerSec: event.stats.hashes_per_second * 12,
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
