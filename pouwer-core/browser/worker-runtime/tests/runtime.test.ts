/// <reference types="vitest" />
import {createWorkerRuntime} from "../src/runtime.js";
import {describe, expect, test} from "vitest";

describe("workerRuntime", () => {
    test("testStartWithoutInitStops", () => {
        const {selfMock, posted} = setupSelf();
        createWorkerRuntime(() => undefined);

        selfMock.onmessage?.({data: {type: "start"}} as MessageEvent);

        expect(posted[0]).toEqual({type: "stopped", reason: "Task was not initialized"});
    });

    test("testStartReportsProgressAndSolved", () => {
        const {selfMock, posted} = setupSelf();
        createWorkerRuntime((_task, runtime) => {
            runtime.reportProgress({attempts: 1, elapsedMs: 10, hashesPerSec: 100});
            runtime.reportSolved({attempts: 2, durationMs: 20, payload: {ok: true}});
        });

        selfMock.onmessage?.({
            data: {type: "init", task: {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}}}
        } as MessageEvent);
        selfMock.onmessage?.({data: {type: "start"}} as MessageEvent);

        expect(posted[0]).toEqual({type: "progress", attempts: 1, elapsedMs: 10, hashesPerSec: 100});
        expect(posted[1]).toEqual({type: "solved", attempts: 2, durationMs: 20, payload: {ok: true}});
    });

    test("testStartReportsStoppedOnError", async () => {
        const {selfMock, posted} = setupSelf();
        createWorkerRuntime(() => {
            throw new Error("boom");
        });

        selfMock.onmessage?.({
            data: {type: "init", task: {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}}}
        } as MessageEvent);
        selfMock.onmessage?.({data: {type: "start"}} as MessageEvent);

        await Promise.resolve();

        expect(posted[0]).toEqual({type: "stopped", reason: "boom"});
    });

    test("testStartIgnoresDuplicateStartAndCancelSuppressesReports", async () => {
        const {selfMock, posted} = setupSelf();
        let resume: (() => void) | null = null;
        let solveCalls = 0;
        createWorkerRuntime(async (_task, runtime) => {
            solveCalls++;
            await new Promise<void>((resolve) => {
                resume = resolve;
            });
            runtime.reportProgress({attempts: 1, elapsedMs: 10, hashesPerSec: 100});
            runtime.reportStopped("stopped");
            runtime.reportSolved({attempts: 2, durationMs: 20, payload: {ok: true}});
        });

        selfMock.onmessage?.({
            data: {type: "init", task: {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}}}
        } as MessageEvent);
        selfMock.onmessage?.({data: {type: "start"}} as MessageEvent);
        selfMock.onmessage?.({data: {type: "start"}} as MessageEvent);
        selfMock.onmessage?.({data: {type: "cancel"}} as MessageEvent);
        resume?.();

        await Promise.resolve();
        await Promise.resolve();

        expect(solveCalls).toBe(1);
        expect(posted).toEqual([]);
    });

    test("testStartReportsStoppedOnStringThrow", async () => {
        const {selfMock, posted} = setupSelf();
        createWorkerRuntime(() => {
            throw "boom";
        });

        selfMock.onmessage?.({
            data: {type: "init", task: {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}}}
        } as MessageEvent);
        selfMock.onmessage?.({data: {type: "start"}} as MessageEvent);

        await Promise.resolve();

        expect(posted[0]).toEqual({type: "stopped", reason: "boom"});
    });

    test("testUnknownMessageTypeIsIgnored", () => {
        const {selfMock, posted} = setupSelf();
        createWorkerRuntime(() => undefined);

        selfMock.onmessage?.({data: {type: "unknown"}} as MessageEvent);

        expect(posted).toEqual([]);
    });

    test("testCanStartAgainAfterSolved", async () => {
        const {selfMock, posted} = setupSelf();
        let calls = 0;
        createWorkerRuntime((_task, runtime) => {
            calls++;
            runtime.reportSolved({attempts: calls, durationMs: 10, payload: {call: calls}});
        });

        selfMock.onmessage?.({
            data: {type: "init", task: {jobId: "1", pluginId: "plugin", expiresAt: 10, payload: {}}}
        } as MessageEvent);

        selfMock.onmessage?.({data: {type: "start"}} as MessageEvent);
        await Promise.resolve();
        selfMock.onmessage?.({data: {type: "start"}} as MessageEvent);
        await Promise.resolve();

        expect(calls).toBe(2);
        expect(posted[0]).toEqual({type: "solved", attempts: 1, durationMs: 10, payload: {call: 1}});
        expect(posted[1]).toEqual({type: "solved", attempts: 2, durationMs: 10, payload: {call: 2}});
    });
});

function setupSelf() {
    const posted: unknown[] = [];
    const selfMock = {
        postMessage: (message: unknown) => posted.push(message),
        onmessage: null as ((event: MessageEvent) => void) | null,
    };
    globalThis.self = selfMock as unknown as WorkerGlobalScope;
    return {selfMock, posted};
}
