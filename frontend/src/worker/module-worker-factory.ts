export const createModuleWorkerFactory = (scriptUrl: string): new () => Worker =>
    class ModuleWorker extends Worker {
        constructor() {
            super(scriptUrl, {type: "module"});
        }
    };
