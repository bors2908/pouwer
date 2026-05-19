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

const randomxWorker = createEmbeddedWorkerFactory("__POUWER_EMBEDDED_MONERO_WORKER", "/assets/monero-worker.js");

export const randomxBinding: PayloadBinding = {
    pluginId: "monero-randomx",
    label: "PoUW (Monero Testnet)",
    workerFactory: randomxWorker,
};
