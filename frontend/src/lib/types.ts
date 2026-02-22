export type HexString = string; // "ab12..."

export interface Challenge {
  nonce: string;           // server-provided opaque nonce (base64 or urlsafe)
  difficulty: number;      // leading-zero bits (integer)
  expiresAt?: number;      // optional epoch ms (for display only)

  // PoUW / Crypto extension
  jobId?: string;
  headerPrefixHex?: string;
  targetHex?: string;
  nonceStart?: number;
  nonceEnd?: number;
}

export interface SolveRequest {
  nonce: string;
  solution: string; // e.g. decimal or hex counter chosen by solver
  hash: HexString;  // hex-encoded sha256(nonce + solution)
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
