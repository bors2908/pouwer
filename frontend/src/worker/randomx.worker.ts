// @ts-ignore
import {mine} from '/randomx-web.js';

import {JobType, RandomXResultPayload, RandomXTask, Task, WorkerInMessage, WorkerOutMessage,} from "../lib/types";

let currentTask: Task | null = null;
let running = false;

self.onmessage = (e: MessageEvent<WorkerInMessage>) => {
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

async function solve(task: Task) {
    switch (task.jobType) {
        case JobType.MONERO_RANDOMX:
            return solveRandomX(task);
        default:
            self.postMessage({type: "stopped", reason: `Unsupported job type: ${task.jobType}`} as WorkerOutMessage);
    }
}

async function solveRandomX(task: RandomXTask) {
    self.postMessage({
        type: "progress",
        attempts: 69,
        elapsedMs: 69,
        hashesPerSec: 69,
    } as WorkerOutMessage);

    mine({
        blob: task.payload.blob,
        job_id: task.jobId,
        target: task.payload.targetHex,
        height: task.payload.height,
        seed_hash: task.payload.seedHash
    })

    self.postMessage({
        type: "progress",
        attempts: 70,
        elapsedMs: 70,
        hashesPerSec: 70,
    } as WorkerOutMessage);


    //self.postMessage({type: "stopped", reason: "Exhausted"} as WorkerOutMessage);

    await new Promise(r => setTimeout(r, 200));
}
