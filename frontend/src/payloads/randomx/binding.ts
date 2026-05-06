import randomxWorker from "./worker.ts?worker";
import {JobType} from "../../contracts";
import type {PayloadBinding} from "../../widget/base/payload-binding";

export const randomxBinding: PayloadBinding = {
    jobType: JobType.MONERO_RANDOMX,
    label: "PoUW (Monero Testnet)",
    workerFactory: randomxWorker,
};
