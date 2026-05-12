import type {BaseTask, NonceRange} from "@pouwer/core-contracts";
import {JobType} from "@pouwer/core-contracts";

export interface Sha256TaskPayload {
    dataHex: string;
    nonceOffset: number;
    nonceIsLE: boolean;
    targetHex: string;
    nonceRange: NonceRange;
}

export interface Sha256PowTask extends BaseTask<JobType.POW_TEST_SHA256, Sha256TaskPayload> {
}

export interface BitcoinSha256Task extends BaseTask<JobType.BITCOIN_RPC_SHA256, Sha256TaskPayload> {
}

export type Sha256Task = Sha256PowTask | BitcoinSha256Task;

export interface Sha256PowResultPayload {
    dataHex: string;
    nonce: number;
    hashHex: string;
    jobType: JobType.POW_TEST_SHA256;
}

export interface BitcoinSha256ResultPayload {
    dataHex: string;
    nonce: number;
    hashHex: string;
    jobType: JobType.BITCOIN_RPC_SHA256;
}

export type Sha256ResultPayload = Sha256PowResultPayload | BitcoinSha256ResultPayload;
