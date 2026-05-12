import {sha256} from "js-sha256";
import {
    BitcoinSha256ResultPayload,
    BitcoinSha256Task,
    Sha256PowResultPayload,
    Sha256PowTask,
    Sha256Task,
} from "./types.js";
import {JobType} from "@pouwer/core-contracts";
import type {BaseTask} from "@pouwer/core-contracts";
import {bytesToHex, createWorkerRuntime, hexToBytes, nowMs, type WorkerRuntimeApi} from "@pouwer/worker-runtime";

type Sha256WorkerResultPayload = Sha256PowResultPayload | BitcoinSha256ResultPayload;

createWorkerRuntime<Sha256Task, Sha256WorkerResultPayload>(solve);

async function solve(task: Sha256Task, runtime: WorkerRuntimeApi<Sha256WorkerResultPayload>) {
    switch (task.jobType) {
        case JobType.POW_TEST_SHA256:
        case JobType.BITCOIN_RPC_SHA256:
            return solveSha256(task, runtime);
        default:
            runtime.reportStopped(`Unsupported job type: ${(task as BaseTask).jobType}`);
    }
}

async function solveSha256(task: Sha256PowTask | BitcoinSha256Task, runtime: WorkerRuntimeApi<Sha256WorkerResultPayload>) {
    const payload = task.payload;
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

    while (runtime.isRunning() && nonce <= nonceEnd) {
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

            const reversedHash = new Uint8Array(hashBytes).reverse();
            const hashBI = BigInt("0x" + bytesToHex(reversedHash));

            if (hashBI <= targetBI) {
                const durationMs = nowMs() - startTime;
                const resultPayload: Sha256PowResultPayload | BitcoinSha256ResultPayload = {
                    dataHex: payload.dataHex,
                    nonce: Number(nonce),
                    hashHex: bytesToHex(reversedHash),
                    jobType: task.jobType,
                };

                runtime.reportSolved({
                    attempts: attempts,
                    durationMs: durationMs,
                    payload: resultPayload,
                });
                return;
            }
            nonce++;
        }

        const currentTime = nowMs();
        if (currentTime - lastProgressTime >= 500) {
            const elapsedMs = currentTime - startTime;
            const hps = Math.floor((attempts * 1000) / elapsedMs);
            runtime.reportProgress({
                attempts,
                elapsedMs,
                hashesPerSec: hps,
            });
            lastProgressTime = currentTime;
        }

        await new Promise((r) => setTimeout(r, 0));
    }

    if (nonce > nonceEnd) {
        runtime.reportStopped("Range exhausted");
    } else {
        runtime.reportStopped("Cancelled");
    }
}
