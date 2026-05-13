import type {BaseTask} from "@pouwer/core-contracts";

export interface RandomXTaskPayload {
    id: string;
    blob: string;
    targetHex: string;
    height: number;
    seedHash: string;
}

export interface RandomXTask extends BaseTask<RandomXTaskPayload> {
}

export interface RandomXResultPayload {
    taskId: string;
    nonce: number;
    hash: string;
}
