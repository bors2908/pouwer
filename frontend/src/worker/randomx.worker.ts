// @ts-ignore
import {randomx_create_vm, randomx_init_cache} from '/randomx-web.js';

import {JobType, RandomXResultPayload, RandomXTask, Task, WorkerInMessage, WorkerOutMessage,} from "../lib/types";

import {nowMs} from "../lib/utils";

type RandomXRuntimeState = {
    cache: any | null;
    vm: any | null;
    seedHash?: string;
};

let currentTask: Task | null = null;
let running = false;

const state: RandomXRuntimeState = {
    cache: null,
    vm: null,
    seedHash: undefined,
};

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
    await ensureRandomXReady(task);

    const taskPayload = task.payload;
    postMessage({type: 'log', payload: `fetched task ${taskPayload.id}`});

    const startTime = nowMs();
    let attempts = 0;
    let lastProgressTime = startTime;

    const maxIterations = 20000;

    const baseBlob = hexToBytes(taskPayload.blob);

    for (let nonce = 0; nonce < maxIterations && running; nonce++) {
        const blobBytes = new Uint8Array(baseBlob);

        // write nonce (uint32 little-endian) at offset 39
        writeNonceLE(blobBytes, nonce, 39);

        // randomx.js calculate_hash accepts string or ArrayBuffer per README
        const hashU8: Uint8Array = state.vm!.calculate_hash(blobBytes);

        attempts++;

        if (nonce % 1000 === 0) {
            postMessage({type: 'progress', payload: `task ${taskPayload.id} nonce ${nonce}`});
        }

        if (meetsTargetCorrect(hashU8, {targetHex: taskPayload.targetHex})) {
            const durationMs = nowMs() - startTime;

            const result: RandomXResultPayload = {
                taskId: taskPayload.id,
                nonce: nonce,
                hash: toHex(hashU8),
                jobType: JobType.MONERO_RANDOMX,
            };

            self.postMessage({
                type: "solved",
                attempts: attempts,
                durationMs: durationMs,
                payload: result,
            } as WorkerOutMessage);

            running = false;
            return;
        }

        const currentTime = nowMs();
        if (currentTime - lastProgressTime >= 500) {
            const elapsedMs = currentTime - startTime;
            const hps = elapsedMs > 0 ? Math.floor((attempts * 1000) / elapsedMs) : 0;

            self.postMessage({
                type: "progress",
                attempts,
                elapsedMs,
                hashesPerSec: hps,
            } as WorkerOutMessage);
            lastProgressTime = currentTime;
        }
    }

    self.postMessage({type: "stopped", reason: "Exhausted"} as WorkerOutMessage);

    await new Promise(r => setTimeout(r, 200));
}

// helper: Uint8Array -> hex string
function toHex(u8: Uint8Array): string {
    return Array.from(u8).map(b => b.toString(16).padStart(2, '0')).join('');
}

function u8ToBigIntLE(bytes: Uint8Array): bigint {
    let v = 0n;
    for (let i = 0; i < bytes.length; i++) {
        v |= (BigInt(bytes[i]) << (8n * BigInt(i)));
    }
    return v;
}

function hexToBytes(hex: string): Uint8Array {
    const clean = hex.startsWith("0x") ? hex.slice(2) : hex;
    if (clean.length % 2 !== 0) {
        throw new Error("Invalid hex length");
    }

    const bytes = new Uint8Array(clean.length / 2);
    for (let i = 0; i < bytes.length; i++) {
        bytes[i] = parseInt(clean.slice(i * 2, i * 2 + 2), 16);
    }
    return bytes;
}

function writeNonceLE(buffer: Uint8Array, nonce: number, offset: number) {
    // uint32 little-endian
    buffer[offset] = nonce & 0xff;
    buffer[offset + 1] = (nonce >> 8) & 0xff;
    buffer[offset + 2] = (nonce >> 16) & 0xff;
    buffer[offset + 3] = (nonce >> 24) & 0xff;
}

function parseDifficulty(d: string | number | bigint): bigint {
    if (typeof d === 'bigint') {
        return d;
    }
    if (typeof d === 'number') {
        return BigInt(Math.floor(d));
    }
    // assume decimal string
    return BigInt(d);
}

function targetFromHexBE(targetHex: string): bigint {
    // normalize
    const hex = targetHex.startsWith('0x') ? targetHex.slice(2) : targetHex;
    if (hex.length > 64) {
        throw new Error('targetHex longer than 32 bytes');
    }
    return BigInt('0x' + hex.padStart(64, '0'));  // big-endian value as bigint
}

function meetsTargetCorrect(
    hash: Uint8Array,
    payload: { targetHex: string }
): boolean {
    const target = targetFromHexBE(payload.targetHex);
    const h = u8ToBigIntLE(hash);
    return h <= target;
}

async function ensureRandomXReady(task: RandomXTask) {
    // Rebuild cache/VM only when the seed changes.
    const seedHash = task.payload.seedHash;

    if (state.vm && state.seedHash === seedHash) {
        return;
    }

    state.seedHash = seedHash;
    state.cache = randomx_init_cache(seedHash);
    state.vm = randomx_create_vm(state.cache);

    postMessage({type: "log", payload: `randomx vm initialized for seed ${seedHash}`});
}
