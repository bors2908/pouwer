import {type PayloadBinding} from "@pouwer/core-contracts";
import {createModuleWorkerFactory} from "@pouwer/worker-runtime";

const sha256Worker = createModuleWorkerFactory("/assets/sha256-worker.js");

export const sha256PowBinding: PayloadBinding = {
    pluginId: "pow-test-sha256",
    label: "Standard PoW",
    workerFactory: sha256Worker,
};

export const bitcoinSha256Binding: PayloadBinding = {
    pluginId: "bitcoin-rpc-sha256",
    label: "PoUW (Bitcoin)",
    workerFactory: sha256Worker,
};
