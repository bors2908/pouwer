import {JobType} from "../../contracts";
import type {PayloadBinding} from "../../widget/base/payload-binding";
import {createModuleWorkerFactory} from "../../worker/module-worker-factory";

const randomxWorker = createModuleWorkerFactory("/assets/randomx-worker.js");

export const randomxBinding: PayloadBinding = {
    jobType: JobType.MONERO_RANDOMX,
    label: "PoUW (Monero Testnet)",
    workerFactory: randomxWorker,
};
