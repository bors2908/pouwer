import {sha256} from "js-sha256";
import {
    BitcoinSha256ResultPayload,
    Sha256PowResultPayload,
    BitcoinSha256Task,
    Sha256PowTask,
    Sha256Task
} from "../lib/sha256/types";
import {JobType} from "../contracts";
import {WorkerInMessage, WorkerOutMessage} from "../core/models";
import {bytesToHex, hexToBytes, nowMs} from "../lib/utils";
import type {BaseTask} from "../contracts";

type Sha256WorkerResultPayload = Sha256PowResultPayload | BitcoinSha256ResultPayload;

let currentTask: Sha256Task | null = null;
let running = false;

self.onmessage = (e: MessageEvent<WorkerInMessage<Sha256Task>>) => {
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

async function solve(task: Sha256Task) {
    switch (task.jobType) {
        case JobType.POW_TEST_SHA256:
        case JobType.BITCOIN_RPC_SHA256:
            return solveSha256(task);
        default:
            self.postMessage({type: "stopped", reason: `Unsupported job type: ${(task as BaseTask).jobType}`} as WorkerOutMessage<Sha256WorkerResultPayload>);
    }
}

async function solveSha256(task: Sha256PowTask | BitcoinSha256Task) {
    const payload = task.payload; // Already typed as Sha256PowTaskPayload due to current TaskPayload definition
    const data = hexToBytes(payload.dataHex);
    const targetBI = BigInt("0x" + payload.targetHex);
    const nonceStart = BigInt(payload.nonceRange.start);
    const nonceEnd = BigInt(payload.nonceRange.end);
    const isLE = payload.nonceIsLE;
    const offset = payload.nonceOffset;

    const startTime = nowMs();
    let attempts = 0;
    let lastProgressTime = startTime;
    let nonce = nonceStart;

    const batchSize = 5000;
    const buffer = new Uint8Array(Math.max(data.length, offset + 4));
    buffer.set(data);

    while (running && nonce <= nonceEnd) {
        for (let i = 0; i < batchSize && nonce <= nonceEnd; i++) {
            attempts++;

            const n = Number(nonce & 0xffffffffn);
            if (isLE) {
                buffer[offset] = n & 0xff;
                buffer[offset + 1] = (n >> 8) & 0xff;
                buffer[offset + 2] = (n >> 16) & 0xff;
                buffer[offset + 3] = (n >> 24) & 0xff;
            } else {
                buffer[offset] = (n >> 24) & 0xff;
                buffer[offset + 1] = (n >> 16) & 0xff;
                buffer[offset + 2] = (n >> 8) & 0xff;
                buffer[offset + 3] = n & 0xff;
            }

            const hash1 = sha256.array(buffer);
            const hash2 = sha256.array(hash1);
            const hashBytes = new Uint8Array(hash2);

            // Bitcoin comparison: reverse bytes to get Little Endian representation if needed,
            // but usually the target is already in the right endianness for comparison.
            // In the backend Sha256Validator: it reverses the computed hash bytes before comparing with target if it was Bitcoin?
            // Actually, Bitcoin targets are 256-bit integers.
            // The hash from double-sha256 is Big Endian (internal repo).
            // Bitcoin RPC expects it reversed.

            const reversedHash = new Uint8Array(hashBytes).reverse();
            const hashBI = BigInt("0x" + bytesToHex(reversedHash));

            if (hashBI <= targetBI) {
                const durationMs = nowMs() - startTime;
                const resultPayload: Sha256PowResultPayload | BitcoinSha256ResultPayload = {
                    dataHex: payload.dataHex,
                    nonce: Number(nonce),
                    hashHex: bytesToHex(reversedHash),
                    jobType: task.jobType
                };

                self.postMessage({
                    type: "solved",
                    attempts: attempts,
                    durationMs: durationMs,
                    payload: resultPayload,
                } as WorkerOutMessage<Sha256WorkerResultPayload>);
                running = false;
                return;
            }
            nonce++;
        }

        const currentTime = nowMs();
        if (currentTime - lastProgressTime >= 500) {
            const elapsedMs = currentTime - startTime;
            const hps = Math.floor((attempts * 1000) / elapsedMs);
            self.postMessage({
                type: "progress",
                attempts,
                elapsedMs,
                hashesPerSec: hps,
            } as WorkerOutMessage<Sha256WorkerResultPayload>);
            lastProgressTime = currentTime;
        }

        await new Promise((r) => setTimeout(r, 0));
    }

    if (nonce > nonceEnd) {
        self.postMessage({type: "stopped", reason: "Range exhausted"} as WorkerOutMessage<Sha256WorkerResultPayload>);
    } else {
        self.postMessage({type: "stopped", reason: "Cancelled"} as WorkerOutMessage<Sha256WorkerResultPayload>);
    }
}
