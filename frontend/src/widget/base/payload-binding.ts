import type {JobType} from "../../contracts";

export interface PayloadBinding {
    readonly jobType: JobType;
    readonly label: string;
    readonly workerFactory: new () => Worker;
}
