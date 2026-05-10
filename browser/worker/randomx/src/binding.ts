import {JobType, type PayloadBinding} from "@core/contracts";
import {createModuleWorkerFactory} from "@worker/runtime";

const randomxWorker = createModuleWorkerFactory("/assets/randomx-worker.js");

export const randomxBinding: PayloadBinding = {
    jobType: JobType.MONERO_RANDOMX,
    label: "PoUW (Monero Testnet)",
    workerFactory: randomxWorker,
};
