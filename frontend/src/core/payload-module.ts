import { JobType } from "../contracts";

export interface PayloadModule {
    readonly jobType: JobType;
    readonly label: string;
    readonly workerFactory: new () => Worker;
    readonly enabled?: boolean;
}
