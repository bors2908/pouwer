import type {Progress} from "@core/contracts";

type ChallengePhase = "ready" | "fetching" | "solving" | "validating";

export class ChallengeUI {
    readonly container: HTMLElement;

    private readonly statusEl: HTMLElement;
    private readonly hpsEl: HTMLElement;
    private readonly attemptsEl: HTMLElement;
    private readonly btnStart: HTMLButtonElement;
    private readonly btnCancel: HTMLButtonElement;
    private readonly resultEl: HTMLElement;

    constructor() {
        const container = document.getElementById("captcha");

        if (!container) {
            throw new Error("Captcha widget markup is missing.");
        }

        const statusEl = document.getElementById("status");
        const hpsEl = document.getElementById("hps");
        const attemptsEl = document.getElementById("attempts");
        const btnStart = document.getElementById("btnStart");
        const btnCancel = document.getElementById("btnCancel");
        const resultEl = document.getElementById("result");

        if (
            !statusEl ||
            !hpsEl ||
            !attemptsEl ||
            !(btnStart instanceof HTMLButtonElement) ||
            !(btnCancel instanceof HTMLButtonElement) ||
            !resultEl
        ) {
            throw new Error("Captcha widget controls are missing.");
        }

        this.container = container;
        this.statusEl = statusEl;
        this.hpsEl = hpsEl;
        this.attemptsEl = attemptsEl;
        this.btnStart = btnStart;
        this.btnCancel = btnCancel;
        this.resultEl = resultEl;

        this.setPhase("ready");
    }

    get challengeUrl(): string {
        return this.container.dataset.challengeUrl || "/challenge";
    }

    onStart(handler: () => void) {
        this.btnStart.onclick = handler;
    }

    onCancel(handler: () => void) {
        this.btnCancel.onclick = handler;
    }

    setPhase(phase: ChallengePhase) {
        switch (phase) {
            case "ready":
                this.updateStatus("Ready");
                this.setControlState(false, true);
                break;
            case "fetching":
                this.updateStatus("Fetching challenge...");
                this.setControlState(true, true);
                break;
            case "solving":
                this.updateStatus("Solving...");
                this.setControlState(true, false);
                break;
            case "validating":
                this.updateStatus("Solved! Validating...");
                this.setControlState(true, true);
                break;
        }
    }

    showError(message: string) {
        this.updateStatus("Error");
        this.setControlState(false, true);
        this.resultEl.textContent = message;
        this.resultEl.style.color = "red";
    }

    showSuccess(message: string) {
        this.updateStatus("Solved");
        this.setControlState(false, true);
        this.resultEl.textContent = message;
        this.resultEl.style.color = "green";
    }

    clearResult() {
        this.resultEl.textContent = "";
        this.resultEl.style.color = "";
    }

    updateProgress(progress: Progress) {
        this.hpsEl.textContent = `Hashes/sec: ${progress.hashesPerSec || 0}`;
        this.attemptsEl.textContent = `Attempts: ${progress.attempts}`;
    }

    private updateStatus(text: string) {
        this.statusEl.textContent = `Status: ${text}`;
    }

    private setControlState(startDisabled: boolean, cancelDisabled: boolean) {
        this.btnStart.disabled = startDisabled;
        this.btnCancel.disabled = cancelDisabled;
    }
}
