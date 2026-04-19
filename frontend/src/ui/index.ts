import {NetworkClient} from "../lib/network";
import {WebWorkerSolver} from "../lib/solver";
import {ISolver, Progress, Task} from "../lib/types";
import {JobType} from "../lib/types";
import {ResultPayload} from "../lib/types";
import {SolveResult} from "../lib/types";

declare global {
    interface Window {
        __CHALLENGE__?: Task;
    }
}

import sha256Worker from "../worker/sha256.worker.ts?worker";
import randomxWorker from "../worker/randomx.worker.ts?worker";
import {ResultMessage} from "../lib/types";

const scriptMap = {
    [JobType.POW_TEST_SHA256]: sha256Worker,
    [JobType.BITCOIN_RPC_SHA256]: sha256Worker,
    [JobType.MONERO_RANDOMX]: randomxWorker,
};

type ChallengeTypeOption = {
    value: JobType;
    label: string;
    enabled?: boolean;
};

const CHALLENGE_TYPE_OPTIONS: readonly ChallengeTypeOption[] = [
    {value: JobType.MONERO_RANDOMX, label: "PoUW (Monero Testnet)"},
    {value: JobType.BITCOIN_RPC_SHA256, label: "PoUW (Bitcoin)", enabled: false},
    {value: JobType.POW_TEST_SHA256, label: "Standard PoW"},
];

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

        this.populateChallengeTypeOptions();

        this.initEvents();
    }

    private populateChallengeTypeOptions() {
        const select = this.uiElements.btnType;
        select.innerHTML = "";
        for (const optionConfig of CHALLENGE_TYPE_OPTIONS) {
            if (optionConfig.enabled === false) {
                continue;
            }

            const optionEl = document.createElement("option");
            optionEl.value = optionConfig.value;
            optionEl.textContent = optionConfig.label;
            select.append(optionEl);
        }
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
                this.uiElements.resultEl.textContent = `Rejected: ${validateRes.reason}`;
                this.uiElements.resultEl.style.color = "red";
            }
        } catch (e: any) {
            if (e.message !== "Cancelled") {
                this.updateStatus("Error");
                this.uiElements.resultEl.textContent = `Error: ${e.message}`;
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

    private getResultMessage(task: Task, result: SolveResult): ResultMessage {
        return {
            jobId: task.jobId,
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
