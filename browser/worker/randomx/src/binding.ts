import {JobType, type PayloadBinding} from "@pouwer/core-contracts";
import {createModuleWorkerFactory} from "@pouwer/worker-runtime";

const randomxWorker = createModuleWorkerFactory("/assets/randomx-worker.js");

export const randomxBinding: PayloadBinding = {
    jobType: JobType.MONERO_RANDOMX,
    label: "PoUW (Monero Testnet)",
    workerFactory: randomxWorker,
};
