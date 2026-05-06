export enum JobType {
    POW_TEST_SHA256 = "POW_TEST_SHA256",
    BITCOIN_RPC_SHA256 = "BITCOIN_RPC_SHA256",
    MONERO_RANDOMX = "MONERO_RANDOMX",
}

export type {
    BaseTask,
    ISolver,
    JobType as CoreJobType,
    NonceRange,
    Progress,
    ResultMessage,
    SolveResult,
    WorkerCancel,
    WorkerInMessage,
    WorkerInit,
    WorkerOutMessage,
    WorkerProgress,
    WorkerSolved,
    WorkerStart,
    WorkerStopped
} from "../core/models";

export type {
    BitcoinSha256ResultPayload,
    BitcoinSha256Task,
    Sha256PowResultPayload,
    Sha256PowTask,
    Sha256ResultPayload,
    Sha256Task,
    Sha256TaskPayload
} from "../lib/sha256/types";

export type {
    RandomXResultPayload,
    RandomXTask,
    RandomXTaskPayload
} from "../lib/randomx/types";

import type {
    BitcoinSha256ResultPayload,
    BitcoinSha256Task,
    Sha256PowResultPayload,
    Sha256PowTask,
    Sha256TaskPayload,
} from "../lib/sha256/types";
import type {RandomXResultPayload, RandomXTask, RandomXTaskPayload} from "../lib/randomx/types";

export type Task = Sha256PowTask | BitcoinSha256Task | RandomXTask;
export type TaskPayload = Sha256TaskPayload | RandomXTaskPayload;
export type ResultPayload = Sha256PowResultPayload | BitcoinSha256ResultPayload | RandomXResultPayload;
