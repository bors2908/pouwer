import {NetworkClient} from "../lib/network.js";
import {WebWorkerSolver} from "../lib/solver.js";
import {ISolver, Progress, Task} from "../lib/types.js";
import {JobType} from "../lib/types.js";
import {ResultPayload} from "../lib/types.js";
import {SolveResult} from "../lib/types.js";

declare global {
    interface Window {
        __CHALLENGE__?: Task;
    }
}

const scriptMap = {
    [JobType.POW_TEST_SHA256]: "/dist/worker/sha256.worker.js",
    [JobType.BITCOIN_RPC_SHA256]: "/dist/worker/sha256.worker.js",
    [JobType.MONERO_RANDOMX]: "/dist/worker/randomx.worker.js",
};

class ChallengeUI {
    task: Task | null = null;

    network: NetworkClient;

    solver: ISolver;

    uiElements: {
        statusEl: HTMLElement;
        hpsEl: HTMLElement;
        attemptsEl: HTMLElement;
        btnStart: HTMLButtonElement;
        btnCancel: HTMLButtonElement;
        btnType: HTMLSelectElement;
        resultEl: HTMLElement;
    };

    constructor() {
        this.network = new NetworkClient();

        // Prefer WebWorker if supported
        if (typeof Worker !== "undefined") {
            this.solver = new WebWorkerSolver(scriptMap);
        } else {
            throw new Error("Workers are not supported in this browser. Main thread solver is not implemented yet.");
        }

        this.uiElements = {
            statusEl: document.getElementById("status")!,
            hpsEl: document.getElementById("hps")!,
            attemptsEl: document.getElementById("attempts")!,
            btnStart: document.getElementById("btnStart") as HTMLButtonElement,
            btnCancel: document.getElementById("btnCancel") as HTMLButtonElement,
            btnType: document.getElementById("challengeType") as HTMLSelectElement,
            resultEl: document.getElementById("result")!,
        };

        this.initEvents();
    }

    private initEvents() {
        this.uiElements.btnStart.onclick = () => this.startSolving();
        this.uiElements.btnCancel.onclick = () => this.cancelSolving();
    }

    private updateStatus(text: string) {
        this.uiElements.statusEl.textContent = `Status: ${text}`;
    }

    private updateProgress(progress: Progress) {
        this.uiElements.hpsEl.textContent = `Hashes/sec: ${progress.hashesPerSec || 0}`;
        this.uiElements.attemptsEl.textContent = `Attempts: ${progress.attempts}`;
    }

    private async startSolving() {
        const type = this.uiElements.btnType.value as JobType;
        this.uiElements.btnStart.disabled = true;
        this.uiElements.btnCancel.disabled = true; // Disabled during fetch
        this.uiElements.btnType.disabled = true;
        this.uiElements.resultEl.textContent = "";
        this.updateStatus("Fetching challenge...");

        try {
            this.task = await this.network.getChallenge(type);

            if (this.task.expiresAt && this.task.expiresAt < Date.now()) {
                this.updateStatus("Challenge expired");
                this.uiElements.btnStart.disabled = false;
                this.uiElements.btnType.disabled = false;
                return;
            }

            this.updateStatus("Solving...");
            this.uiElements.btnCancel.disabled = false;

            const result = await this.solver.start(this.task, (p) => this.updateProgress(p));
            this.updateStatus("Solved! Validating...");
            this.updateProgress({
                attempts: result.attempts,
                elapsedMs: result.durationMs,
                hashesPerSec: Math.floor((result.attempts * 1000) / result.durationMs),
            });

            const resultMessage = this.getResultMessage(this.task, result)

            const validateRes = await this.network.postValidate(resultMessage);

            if (validateRes.ok) {
                this.updateStatus("Success!");
                this.uiElements.resultEl.textContent = "OK (200)";
                this.uiElements.resultEl.style.color = "green";
            } else {
                this.updateStatus("Failed");
                // @ts-ignore
                this.uiElements.resultEl.textContent = `Rejected: ${validateRes.reason}`;
                this.uiElements.resultEl.style.color = "red";
            }
        } catch (e: any) {
            if (e.message !== "Cancelled") {
                this.updateStatus("Error");
                let errorMsg = "Unknown error";
                if (e instanceof Error) {
                    errorMsg = e.message;
                } else if (typeof e === "string") {
                    errorMsg = e;
                } else if (e && e.message) {
                    errorMsg = String(e.message);
                } else if (e && e.type === "error" && e.target instanceof Worker) {
                    errorMsg = "Worker error (check console)";
                } else {
                    try {
                        errorMsg = JSON.stringify(e);
                    } catch {
                        errorMsg = String(e);
                    }
                }
                this.uiElements.resultEl.textContent = `Error: ${errorMsg}`;
                this.uiElements.resultEl.style.color = "red";
            } else {
                this.updateStatus("Ready");
            }
        } finally {
            this.uiElements.btnStart.disabled = false;
            this.uiElements.btnCancel.disabled = true;
            this.uiElements.btnType.disabled = false;
        }
    }

    private getResultMessage(task: Task, result: SolveResult): {
        jobId: string;
        jobType: JobType;
        payload: ResultPayload;
        leaseHmac: string | undefined;
        attempts: number;
        durationMs: number
    } {
        return {
            jobId: task.jobId,
            jobType: task.jobType,
            payload: result.payload,
            leaseHmac: task.leaseHmac,
            attempts: result.attempts,
            durationMs: result.durationMs
        };
    }

    private cancelSolving() {
        this.solver.cancel();
        this.updateStatus("Ready");
        this.uiElements.btnStart.disabled = false;
        this.uiElements.btnCancel.disabled = true;
        this.uiElements.btnType.disabled = false;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    new ChallengeUI();
});
