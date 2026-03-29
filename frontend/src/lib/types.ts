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

export interface BaseResultMessage {
    jobId: string;
    jobType: JobType;
    meta?: Record<string, string>;
    leaseHmac?: string;
    attempts: number;
    durationMs: number;
}

export interface Sha256PowResultMessage extends BaseResultMessage {
    jobType: JobType.POW_TEST_SHA256;
    payload: Sha256PowResultPayload;
}

export interface RandomXResultMessage extends BaseResultMessage {
    jobType: JobType.MONERO_RANDOMX;
    payload: RandomXResultPayload;
}

export type ResultMessage =
    | Sha256PowResultMessage
    | RandomXResultMessage;

export interface BaseSolveResult {
    attempts: number;
    durationMs: number;
    jobType: JobType;
}

export interface Sha256SolveResult extends BaseSolveResult {
    jobType: JobType.POW_TEST_SHA256;
    payload: Sha256PowResultPayload;
}

export interface RandomXSolveResult extends BaseSolveResult {
    jobType: JobType.MONERO_RANDOMX;
    payload: RandomXResultPayload;
}

export type SolveResult =
    | Sha256SolveResult
    | RandomXSolveResult;

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

export interface Sha256PowResultPayload {
    dataHex: string;
    nonce: number;
    hashHex: string;
}

export interface RandomXResultPayload {
    taskId: string;
    nonce: number;
    hash: string;
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
