import { JobType } from "./models";

export interface PayloadModule {
    readonly jobType: JobType;
    readonly label: string;
    readonly workerFactory: new () => Worker;
    readonly enabled?: boolean;
}
