import type {JobType} from "./job-types.js";

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

export interface BaseTask<TJobType extends JobType = JobType, TPayload = unknown> {
    jobId: string;
    jobType: TJobType;
    expiresAt: number;
    leaseHmac?: string;
    payload: TPayload;
}

export interface ResultMessage<TPayload = unknown> {
    jobId: string;
    meta?: Record<string, string>;
    leaseHmac?: string;
    attempts: number;
    durationMs: number;
    payload: TPayload;
}

export interface SolveResult<TPayload = unknown> {
    attempts: number;
    durationMs: number;
    payload: TPayload;
}

export interface ISolver<TTask extends BaseTask = BaseTask, TResultPayload = unknown> {
    start(challenge: TTask, onProgress?: (stats: Progress) => void): Promise<SolveResult<TResultPayload>>;
    cancel(jobType?: TTask["jobType"]): void;
}

export type WorkerInit<TTask extends BaseTask = BaseTask> = { type: "init"; task: TTask };
export type WorkerCancel = { type: "cancel" };
export type WorkerStart = { type: "start" };

export type WorkerProgress = { type: "progress"; attempts: number; elapsedMs: number; hashesPerSec: number };
export type WorkerSolved<TResultPayload = unknown> = { type: "solved" } & SolveResult<TResultPayload>;
export type WorkerStopped = { type: "stopped"; reason?: string };

export type WorkerInMessage<TTask extends BaseTask = BaseTask> = WorkerInit<TTask> | WorkerCancel | WorkerStart;
export type WorkerOutMessage<TResultPayload = unknown> = WorkerProgress | WorkerSolved<TResultPayload> | WorkerStopped;
