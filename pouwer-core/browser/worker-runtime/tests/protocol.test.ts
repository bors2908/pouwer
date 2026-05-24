/// <reference types="vitest" />
import type {BaseTask, ResultMessage, WorkerInMessage, WorkerOutMessage} from "@pouwer/core-contracts";
import {describe, expect, test} from "vitest";

describe("protocolMessages", () => {
    test("testTaskRoundTripThroughJson", () => {
        const task: BaseTask<{seed: string; difficulty: number}> = {
            jobId: "job-1",
            pluginId: "pow-test-sha256",
            expiresAt: 1700000000000,
            leaseHmac: "hmac-1",
            payload: {seed: "abcd", difficulty: 18},
        };

        const roundTripped = JSON.parse(JSON.stringify(task)) as BaseTask<{seed: string; difficulty: number}>;

        expect(roundTripped).toEqual(task);
    });

    test("testResultMessageRoundTripThroughJson", () => {
        const result: ResultMessage<{nonce: number; hashHex: string}> = {
            jobId: "job-1",
            pluginId: "pow-test-sha256",
            leaseHmac: "hmac-1",
            attempts: 321,
            durationMs: 5000,
            payload: {nonce: 42, hashHex: "00ff"},
            meta: {worker: "w1"},
        };

        const roundTripped = JSON.parse(JSON.stringify(result)) as ResultMessage<{nonce: number; hashHex: string}>;

        expect(roundTripped).toEqual(result);
    });

    test("testWorkerInMessageSupportsAllVariants", () => {
        const init: WorkerInMessage<BaseTask> = {
            type: "init",
            task: {jobId: "1", pluginId: "p", expiresAt: 1, payload: {}},
        };
        const start: WorkerInMessage<BaseTask> = {type: "start"};
        const cancel: WorkerInMessage<BaseTask> = {type: "cancel"};

        expect(init.type).toBe("init");
        expect(start.type).toBe("start");
        expect(cancel.type).toBe("cancel");
    });

    test("testWorkerOutMessageSupportsAllVariants", () => {
        const progress: WorkerOutMessage<{ok: true}> = {type: "progress", attempts: 1, elapsedMs: 10, hashesPerSec: 100};
        const solved: WorkerOutMessage<{ok: true}> = {type: "solved", attempts: 2, durationMs: 20, payload: {ok: true}};
        const stopped: WorkerOutMessage<{ok: true}> = {type: "stopped", reason: "done"};

        expect(progress.type).toBe("progress");
        expect(solved.type).toBe("solved");
        expect(stopped.type).toBe("stopped");
    });
});
