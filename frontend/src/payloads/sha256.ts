import sha256Worker from "../worker/sha256.worker.ts?worker";
import {JobType} from "../contracts";
import {PayloadModule} from "../core/payload-module";

export const sha256PowModule: PayloadModule = {
    jobType: JobType.POW_TEST_SHA256,
    label: "Standard PoW",
    workerFactory: sha256Worker,
};

export const bitcoinSha256Module: PayloadModule = {
    jobType: JobType.BITCOIN_RPC_SHA256,
    label: "PoUW (Bitcoin)",
    workerFactory: sha256Worker,
    enabled: false,
};
