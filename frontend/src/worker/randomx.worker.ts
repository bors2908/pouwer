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

import {JobType} from "../contracts";
import {WorkerInMessage, WorkerOutMessage} from "../core/models";
import {RandomXResultPayload, RandomXTask} from "../lib/randomx/types";
import {nowMs} from "../lib/utils";

let currentTask: RandomXTask | null = null;
let running = false;

self.onmessage = (e: MessageEvent<WorkerInMessage<RandomXTask>>) => {
    const msg = e.data;
    switch (msg.type) {
        case "init":
            currentTask = msg.task;
            break;
        case "start":
            if (currentTask) {
                running = true;
                solve(currentTask);
            }
            break;
        case "cancel":
            running = false;
            break;
    }
};

async function solve(task: RandomXTask) {
    switch (task.jobType) {
        case JobType.MONERO_RANDOMX:
            return solveRandomX(task);
        default:
            self.postMessage({type: "stopped", reason: `Unsupported job type: ${task.jobType}`} as WorkerOutMessage<RandomXResultPayload>);
    }
}

async function solveRandomX(task: RandomXTask) {
    const job: Job = {
        blob: task.payload.blob,
        job_id: task.jobId,
        target: task.payload.targetHex,
        height: task.payload.height,
        seed_hash: task.payload.seedHash
    };
    const taskPayload = task.payload;
    const startTime = nowMs();

    const callbacks: MinerCallbacks = {
        on_cache_initialising: () => {
            console.log(`Cache Initialising...`)
        },
        on_cache_initialised: (duration_ms: number) => {
            console.log(`Cache Initialised in ${duration_ms}ms`)
        },
        on_worker_ready: (event: WorkerEventWorkerReady) => {
            console.log(`Worker ${event.miner_id} ready`)
        },
        on_job_started: (event: WorkerEventJobStarted) => {
            console.log(`Job ${event.job_id} started on worker ${event.miner_id}`)
        },
        on_job_disposed: (event: WorkerEventJobDisposed) => {
            console.log(`Job disposed on worker ${event.miner_id}`)
        },
        on_nonce_space_exhausted: (event: WorkerEventNonceSpaceExhausted) => {
            self.postMessage({type: "stopped", reason: "Exhausted"} as WorkerOutMessage<RandomXResultPayload>);
        },
        on_result_found: (event: WorkerEventResultFound) => {
            const durationMs = nowMs() - startTime;

            console.log(`Result found: ${toHex(event.result)}`)

            const result: RandomXResultPayload = {
                taskId: taskPayload.id,
                nonce: event.nonce,
                hash: toHex(event.result),
                jobType: JobType.MONERO_RANDOMX,
            };

            self.postMessage({
                type: "solved",
                attempts: event.hash_count,
                durationMs: durationMs,
                payload: result,
            } as WorkerOutMessage<RandomXResultPayload>);

            running = false;
            return;
        },
        on_pong: (event: WorkerPong) => {
            const elapsedMs = nowMs() - startTime;

            console.log(`Pong: ${event.stats.hashes_total} hashes in ${elapsedMs}ms`)

            self.postMessage({
                type: "progress",
                attempts: event.stats.hashes_total,
                elapsedMs,
                //TODO: Fix, combine multiple pongs instead of this.
                hashesPerSec: event.stats.hashes_per_second * 12,
            } as WorkerOutMessage<RandomXResultPayload>)
        },
    }

    const config: Config = {
        callbacks: callbacks
    }

    mine(job, config)

    await new Promise(r => setTimeout(r, 200));
}

function toHex(u8: Uint8Array): string {
    return Array.from(u8).map(b => b.toString(16).padStart(2, '0')).join('');
}
