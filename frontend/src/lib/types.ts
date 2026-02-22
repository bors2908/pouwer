export type HexString = string; // "ab12..."

export enum JobType {
  POW_TEST_SHA256 = "POW_TEST_SHA256",
  BITCOIN_RPC_SHA256 = "BITCOIN_RPC_SHA256",
}

export interface NonceRange {
  start: number;
  end: number;
}

export interface Sha256PowTaskPayload {
  dataHex: string;
  nonceOffset: number;
  nonceIsLE: boolean;
  targetHex: string;
  nonceRange: NonceRange;
}

export type TaskPayload = Sha256PowTaskPayload;

export interface Task {
  jobId: string;
  jobType: JobType;
  expiresAt: number;
  payload: TaskPayload;
  leaseHmac?: string;
}

export interface Sha256PowResultPayload {
  dataHex: string;
  nonce: number;
  hashHex: string;
  durationMs?: number;
  attempts?: number;
}

export type ResultPayload = Sha256PowResultPayload;

export interface ResultMessage {
  jobId: string;
  jobType: JobType;
  payload: ResultPayload;
  meta?: Record<string, string>;
  leaseHmac?: string;
}

// Deprecated in favor of Task
export type Challenge = Task;

export interface SolveRequest {
  jobId: string;
  jobType: JobType;
  payload: ResultPayload;
  leaseHmac?: string;
}

export type ValidateResponse = { ok: true } | { ok: false; reason?: string };

export interface Progress {
  attempts: number;
  hashesPerSec?: number;
  elapsedMs: number;
  lastHashHex?: string;
}

export interface SolveResult {
  solution: string;
  hashHex: string;
  attempts: number;
  durationMs: number;
  nonce?: number; // for crypto
}

export interface ISolver {
  start(challenge: Challenge, onProgress?: (stats: Progress) => void): Promise<SolveResult>;
  cancel(): void;
}

// Worker message types
export type WorkerInit = { type: "init"; challenge: Challenge };
export type WorkerCancel = { type: "cancel" };
export type WorkerStart = { type: "start" };

export type WorkerProgress = { type: "progress"; attempts: number; elapsedMs: number; hashesPerSec: number };
export type WorkerSolved = { type: "solved" } & SolveResult;
export type WorkerStopped = { type: "stopped"; reason?: string };

export type WorkerInMessage = WorkerInit | WorkerCancel | WorkerStart;
export type WorkerOutMessage = WorkerProgress | WorkerSolved | WorkerStopped;
