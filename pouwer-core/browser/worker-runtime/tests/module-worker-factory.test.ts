/// <reference types="vitest" />
import {createModuleWorkerFactory} from "../src/module-worker-factory.js";
import {describe, expect, test} from "vitest";

describe("moduleWorkerFactory", () => {
    test("testCreatesModuleWorkerWithScript", () => {
        const originalWorker = globalThis.Worker;
        class FakeWorker {
            static lastArgs: {scriptUrl: string; options: WorkerOptions} | null = null;
            constructor(scriptUrl: string, options: WorkerOptions) {
                FakeWorker.lastArgs = {scriptUrl, options};
            }
        }
        globalThis.Worker = FakeWorker as unknown as typeof Worker;

        const WorkerFactory = createModuleWorkerFactory("worker.js");
        new WorkerFactory();

        expect(FakeWorker.lastArgs).toEqual({scriptUrl: "worker.js", options: {type: "module"}});

        globalThis.Worker = originalWorker;
    });
});
