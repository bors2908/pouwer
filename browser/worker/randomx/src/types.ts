import type {BaseTask} from "@pouwer/core-contracts";
import {JobType} from "@pouwer/core-contracts";

export interface RandomXTaskPayload {
    id: string;
    blob: string;
    targetHex: string;
    height: number;
    seedHash: string;
}

export interface RandomXTask extends BaseTask<JobType.MONERO_RANDOMX, RandomXTaskPayload> {
}

export interface RandomXResultPayload {
    taskId: string;
    nonce: number;
    hash: string;
    jobType: JobType.MONERO_RANDOMX;
}
