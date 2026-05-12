import type {JobType} from "./job-types.js";

export interface PayloadBinding {
    readonly jobType: JobType;
    readonly label: string;
    readonly workerFactory: new () => Worker;
}
