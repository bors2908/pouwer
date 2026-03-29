import {randomx_create_vm, randomx_init_cache} from 'randomx.js';
import {JobType, Task, WorkerInMessage, WorkerOutMessage} from "../lib/types.js";
import {RandomXResultPayload} from "../lib/types.js";
import {RandomXTask} from "../lib/types.js";
import {nowMs} from "../lib/utils.js";

let currentTask: Task | null = null;
let running = false;
let vm: any = null;

self.onmessage = (e: MessageEvent<WorkerInMessage>) => {
    handleMessage(e.data);
};

function handleMessage(msg: WorkerInMessage) {
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
}

function postMessageToParent(msg: WorkerOutMessage | { type: string; payload: string }) {
    (self as any).postMessage(msg);
}

async function solve(task: Task) {
    switch (task.jobType) {
        case JobType.MONERO_RANDOMX:
            return solveRandomX(task);
        default:
            postMessageToParent({type: "stopped", reason: `Unsupported job type: ${task.jobType}`} as WorkerOutMessage);
    }
}

async function solveRandomX(task: RandomXTask) {
    if (!vm) {
        await initRandomx();
    }

    const taskPayload = task.payload
    postMessageToParent({type: 'log', payload: `fetched task ${taskPayload.id}`});

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
        const hashU8: Uint8Array = vm.calculate_hash(blobBytes);

        attempts++;

        if (nonce % 1000 === 0) {
            postMessageToParent({type: 'progress', payload: `task ${taskPayload.id} nonce ${nonce}`});
        }

        if (meetsTargetCorrect(hashU8, {difficulty: taskPayload.difficulty, targetHex: taskPayload.targetHex})) {
            const durationMs = nowMs() - startTime;

            const hex = toHex(hashU8);
            const result: RandomXResultPayload = {
                taskId: taskPayload.id,
                nonce,
                hash: hex
            };

            postMessageToParent({
                type: "solved",
                jobType: task.jobType,
                attempts: attempts,
                durationMs: durationMs,
                payload: result,
            } as any);
            running = false;
            return;
        }

        const currentTime = nowMs();
        if (currentTime - lastProgressTime >= 500) {
            const elapsedMs = currentTime - startTime;
            const hps = Math.floor((attempts * 1000) / elapsedMs);
            postMessageToParent({
                type: "progress",
                attempts,
                elapsedMs,
                hashesPerSec: hps,
            } as WorkerOutMessage);
            lastProgressTime = currentTime;
        }
    }

    postMessageToParent({type: "stopped", reason: "Exhausted"} as WorkerOutMessage);

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
        bytes[i] = parseInt(clean.substr(i * 2, 2), 16);
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

function targetFromDifficulty(difficulty: bigint): bigint {
    if (difficulty <= 0n) {
        throw new Error('invalid difficulty');
    }
    const max = (1n << 256n) - 1n;
    return max / difficulty;
}

function targetFromHexBE(targetHex: string): bigint {
    // normalize
    const hex = targetHex.startsWith('0x') ? targetHex.slice(2) : targetHex;
    if (hex.length > 64) {
        throw new Error('targetHex longer than 32 bytes');
    }
    const be = BigInt('0x' + hex.padStart(64, '0')); // big-endian value as bigint
    return be;
}

function meetsTargetCorrect(
    hash: Uint8Array,
    payload: { difficulty?: string | number | bigint; targetHex?: string }
): boolean {
    // prefer explicit targetHex if present
    if (payload.targetHex) {
        const target = targetFromHexBE(payload.targetHex);
        const h = u8ToBigIntLE(hash);
        return h <= target;
    }

    if (!payload.difficulty) {
        // permissive for dev/testing; change to false in production
        return true;
    }

    const difficulty = parseDifficulty(payload.difficulty);
    const target = targetFromDifficulty(difficulty);
    const h = u8ToBigIntLE(hash);
    return h <= target;
}

async function initRandomx() {
    const cache = randomx_init_cache('demo-key');
    vm = randomx_create_vm(cache);
    postMessageToParent({type: 'log', payload: 'randomx vm initialized'});
}
