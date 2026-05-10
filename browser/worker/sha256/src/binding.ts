import {JobType, type PayloadBinding} from "@core/contracts";
import {createModuleWorkerFactory} from "@worker/runtime";

const sha256Worker = createModuleWorkerFactory("/assets/sha256-worker.js");

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
