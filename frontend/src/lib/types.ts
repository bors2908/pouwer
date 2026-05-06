export * from "../core/models";
export * from "./sha256/types";
export * from "./randomx/types";

import type {
    BitcoinSha256ResultPayload,
    BitcoinSha256Task,
    Sha256PowResultPayload,
    Sha256PowTask
} from "./sha256/types";
import type {RandomXResultPayload, RandomXTask} from "./randomx/types";

export type Task = Sha256PowTask | BitcoinSha256Task | RandomXTask;
export type TaskPayload = Sha256PowTask["payload"] | BitcoinSha256Task["payload"] | RandomXTask["payload"];
export type ResultPayload = Sha256PowResultPayload | BitcoinSha256ResultPayload | RandomXResultPayload;
