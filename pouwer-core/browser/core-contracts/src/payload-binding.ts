export interface PayloadBinding {
    readonly pluginId: string;
    readonly label: string;
    readonly workerFactory: new () => Worker;
}
