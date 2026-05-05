import randomxWorker from "../worker/randomx.worker.ts?worker";
import {JobType} from "../contracts";
import {PayloadModule} from "../core/payload-module";

export const randomxModule: PayloadModule = {
    jobType: JobType.MONERO_RANDOMX,
    label: "PoUW (Monero Testnet)",
    workerFactory: randomxWorker,
};
