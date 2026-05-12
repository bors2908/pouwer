import {type PayloadBinding} from "@pouwer/core-contracts";
import {createModuleWorkerFactory} from "@pouwer/worker-runtime";

const randomxWorker = createModuleWorkerFactory("/assets/randomx-worker.js");

export const randomxBinding: PayloadBinding = {
    pluginId: "monero-randomx",
    label: "PoUW (Monero Testnet)",
    workerFactory: randomxWorker,
};
