import {type PayloadBinding} from "@pouwer/core-contracts";
import {createModuleWorkerFactory} from "@pouwer/worker-runtime";

function createEmbeddedWorkerFactory(globalVarName: string, fallbackUrl: string): new () => Worker {
    if ((globalThis as any)[globalVarName] && typeof (globalThis as any)[globalVarName] === "string") {
        const script = (globalThis as any)[globalVarName] as string;
        return class ModuleWorker extends Worker {
            constructor() {
                const blob = new Blob([script], { type: "text/javascript" });
                super(URL.createObjectURL(blob), { type: "module" });
            }
        };
    }
    return createModuleWorkerFactory(fallbackUrl);
}

const sha256Worker = createEmbeddedWorkerFactory("__POUWER_EMBEDDED_SHA256_WORKER", "/assets/sha256-worker.js");

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
