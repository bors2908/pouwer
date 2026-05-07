import {JobType} from "../../contracts";
import type {PayloadBinding} from "../../widget/base/payload-binding";
import {createModuleWorkerFactory} from "../../worker/module-worker-factory";

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
