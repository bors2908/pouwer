export enum JobType {
    POW_TEST_SHA256 = "POW_TEST_SHA256",
    BITCOIN_RPC_SHA256 = "BITCOIN_RPC_SHA256",
    MONERO_RANDOMX = "MONERO_RANDOMX",
}

export type ValidateResponse = { ok: true } | { ok: false; reason?: string };

export interface NonceRange {
    start: number;
    end: number;
}

export interface Progress {
    attempts: number;
    hashesPerSec?: number;
    elapsedMs: number;
    lastHashHex?: string;
}

export interface BaseTask {
    jobId: string;
    jobType: JobType;
    expiresAt: number;
    leaseHmac?: string;
}

export interface Sha256PowTask extends BaseTask {
    jobType: JobType.POW_TEST_SHA256;
    payload: Sha256PowTaskPayload;
}

export interface RandomXTask extends BaseTask {
    jobType: JobType.MONERO_RANDOMX;
    payload: RandomXTaskPayload;
}

export type Task = Sha256PowTask | RandomXTask;

export interface ResultMessage {
    jobId: string;
    meta?: Record<string, string>;
    leaseHmac?: string;
    attempts: number;
    durationMs: number;
    payload: ResultPayload;
}

export interface SolveResult {
    attempts: number;
    durationMs: number;
    payload: ResultPayload;
}

export interface Sha256PowTaskPayload {
    dataHex: string;
    nonceOffset: number;
    nonceIsLE: boolean;
    targetHex: string;
    nonceRange: NonceRange;
}

export interface RandomXTaskPayload {
    id: string;
    blob: string;
    difficulty?: string;
    targetHex?: string;
}

export interface ResultPayloadBase {
    jobType: JobType;
}

export interface Sha256PowResultPayload extends ResultPayloadBase {
    dataHex: string;
    nonce: number;
    hashHex: string;
    jobType: JobType.POW_TEST_SHA256;
}

export interface RandomXResultPayload extends ResultPayloadBase {
    taskId: string;
    nonce: number;
    hash: string;
    jobType: JobType.MONERO_RANDOMX;
}

export type TaskPayload = Sha256PowTaskPayload | RandomXTaskPayload;

export type ResultPayload = Sha256PowResultPayload | RandomXResultPayload;

export interface ISolver {
    start(challenge: Task, onProgress?: (stats: Progress) => void): Promise<SolveResult>;

    cancel(): void;
}

// Worker message types
export type WorkerInit = { type: "init"; task: Task };
export type WorkerCancel = { type: "cancel" };
export type WorkerStart = { type: "start" };

export type WorkerProgress = { type: "progress"; attempts: number; elapsedMs: number; hashesPerSec: number };
export type WorkerSolved = { type: "solved" } & SolveResult;
export type WorkerStopped = { type: "stopped"; reason?: string };

export type WorkerInMessage = WorkerInit | WorkerCancel | WorkerStart;
export type WorkerOutMessage = WorkerProgress | WorkerSolved | WorkerStopped;
