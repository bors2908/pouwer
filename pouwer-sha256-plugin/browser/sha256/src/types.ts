import type {BaseTask, NonceRange} from "@pouwer/core-contracts";

export interface Sha256TaskPayload {
    dataHex: string;
    nonceOffset: number;
    nonceIsLE: boolean;
    targetHex: string;
    nonceRange: NonceRange;
}

export interface Sha256PowTask extends BaseTask<Sha256TaskPayload> {
}

export interface BitcoinSha256Task extends BaseTask<Sha256TaskPayload> {
}

export type Sha256Task = Sha256PowTask | BitcoinSha256Task;

export interface Sha256PowResultPayload {
    nonce: number;
    hashHex: string;
}

export interface BitcoinSha256ResultPayload {
    nonce: number;
    hashHex: string;
}

export type Sha256ResultPayload = Sha256PowResultPayload | BitcoinSha256ResultPayload;
