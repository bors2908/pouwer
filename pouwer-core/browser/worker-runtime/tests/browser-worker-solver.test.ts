/// <reference types="vitest" />
import {WebWorkerSolver} from "../src/browser-worker-solver.js";
import {describe, expect, test} from "vitest";

describe("webWorkerSolver", () => {
    test("testStartResolvesOnSolved", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}};
        const progress: any[] = [];

        const promise = solver.start(task, (stats) => progress.push(stats));
        const worker = FakeWorker.instances[0];

        expect(worker.messages).toEqual([{type: "init", task}, {type: "start"}]);

        worker.emitMessage({type: "progress", attempts: 1, elapsedMs: 10, hashesPerSec: 100});
        worker.emitMessage({type: "solved", attempts: 2, durationMs: 20, payload: {ok: true}});

        const result = await promise;
        expect(result.type).toBe("solved");
        expect(progress).toHaveLength(1);
        expect(worker.terminated).toBe(true);
        expect(solver.isRunning(task.pluginId)).toBe(false);
    });

    test("testStartRejectsOnStopped", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}};

        const promise = solver.start(task);
        const worker = FakeWorker.instances[0];
        worker.emitMessage({type: "stopped", reason: "nope"});

        await expect(promise).rejects.toThrow("nope");
        expect(worker.terminated).toBe(true);
    });

    test("testStartRejectsOnErrorEvent", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}};

        const promise = solver.start(task);
        const worker = FakeWorker.instances[0];
        worker.emitError("boom");

        await expect(promise).rejects.toThrow("boom");
        expect(worker.terminated).toBe(true);
    });

    test("testCancelRejectsPromise", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}};

        const promise = solver.start(task);
        const worker = FakeWorker.instances[0];

        solver.cancel(task.pluginId);

        await expect(promise).rejects.toThrow("Cancelled");
        expect(worker.messages).toContainEqual({type: "cancel"});
        expect(worker.terminated).toBe(true);
    });

    test("testStartRejectsWhenAlreadyRunning", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}};

        const first = solver.start(task);

        await expect(solver.start(task)).rejects.toThrow("already running");

        const worker = FakeWorker.instances[0];
        worker.emitMessage({type: "solved", attempts: 1, durationMs: 10, payload: {}});
        await first;
    });

    test("testCancelWithoutRunningWorkerIsNoOp", () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);

        solver.cancel("plugin");

        expect(FakeWorker.instances).toHaveLength(0);
    });

    test("testStartRejectsOnStoppedWithoutReason", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}};

        const promise = solver.start(task);
        const worker = FakeWorker.instances[0];
        worker.emitMessage({type: "stopped", reason: ""});

        await expect(promise).rejects.toThrow("Stopped");
        expect(worker.terminated).toBe(true);
    });

    test("testStartRejectsOnErrorEventWithoutMessage", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}};

        const promise = solver.start(task);
        const worker = FakeWorker.instances[0];
        worker.emitError("");

        await expect(promise).rejects.toThrow("Worker error");
        expect(worker.terminated).toBe(true);
    });

    test("testCancelCancelsRegardlessOfPluginIdArgument", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin-a", expiresAt: 10, payload: {}};

        const promise = solver.start(task);
        solver.cancel("plugin-b");

        await expect(promise).rejects.toThrow("Cancelled");
        expect(FakeWorker.instances[0].terminated).toBe(true);
    });

    test("testProgressMessageWithoutCallbackDoesNotFail", async () => {
        FakeWorker.instances = [];
        const solver = new WebWorkerSolver(FakeWorker as unknown as new () => Worker);
        const task = {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}};

        const promise = solver.start(task);
        const worker = FakeWorker.instances[0];
        worker.emitMessage({type: "progress", attempts: 1, elapsedMs: 10, hashesPerSec: 100});
        worker.emitMessage({type: "solved", attempts: 2, durationMs: 20, payload: {ok: true}});

        await expect(promise).resolves.toEqual({type: "solved", attempts: 2, durationMs: 20, payload: {ok: true}});
    });
});

class FakeWorker {
    static instances: FakeWorker[] = [];
    onmessage: ((event: MessageEvent) => void) | null = null;
    onerror: ((event: ErrorEvent) => void) | null = null;
    messages: unknown[] = [];
    terminated = false;

    constructor() {
        FakeWorker.instances.push(this);
    }

    postMessage(message: unknown) {
        this.messages.push(message);
    }

    terminate() {
        this.terminated = true;
    }

    emitMessage(data: unknown) {
        this.onmessage?.({data} as MessageEvent);
    }

    emitError(message: string) {
        this.onerror?.({message} as ErrorEvent);
    }
}
