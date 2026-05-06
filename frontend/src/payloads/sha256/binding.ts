import sha256Worker from "./worker.ts?worker";
import {JobType} from "../../contracts";
import type {PayloadBinding} from "../../widget/base/payload-binding";

export const sha256PowBinding: PayloadBinding = {
    jobType: JobType.POW_TEST_SHA256,
    label: "Standard PoW",
    workerFactory: sha256Worker,
};

export const bitcoinSha256Binding: PayloadBinding = {
    jobType: JobType.BITCOIN_RPC_SHA256,
    label: "PoUW (Bitcoin)",
    workerFactory: sha256Worker,
};
