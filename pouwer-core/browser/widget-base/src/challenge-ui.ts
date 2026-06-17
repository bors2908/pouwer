import type {Progress} from "@pouwer/core-contracts";

type ChallengePhase = "ready" | "fetching" | "solving" | "validating";
type IndicatorState = "idle" | "working" | "success" | "error";

export class ChallengeUI {
    readonly container: HTMLElement;

    private readonly statusEl: HTMLElement;
    private readonly hpsEl: HTMLElement;
    private readonly attemptsEl: HTMLElement;
    private readonly btnAction: HTMLButtonElement;
    private readonly resultEl: HTMLElement;
    private readonly resultSection: HTMLElement;
    private readonly indicator: HTMLElement;

    private phase: ChallengePhase = "ready";
    private startHandler?: () => void;
    private cancelHandler?: () => void;

    constructor() {
        const container = document.getElementById("captcha");

        if (!container) {
            throw new Error("Captcha widget markup is missing.");
        }

        const statusEl = document.getElementById("status");
        const hpsEl = document.getElementById("hps");
        const attemptsEl = document.getElementById("attempts");
        const btnStart = document.getElementById("btnStart");
        const resultEl = document.getElementById("result");
        const resultSection = document.getElementById("result-section");
        const indicator = document.getElementById("captcha-indicator");
        const btnStats = document.getElementById("btnStats");

        if (
            !statusEl ||
            !hpsEl ||
            !attemptsEl ||
            !(btnStart instanceof HTMLButtonElement) ||
            !resultEl ||
            !resultSection ||
            !indicator
        ) {
            throw new Error("Captcha widget controls are missing.");
        }

        this.container = container;
        this.statusEl = statusEl;
        this.hpsEl = hpsEl;
        this.attemptsEl = attemptsEl;
        this.btnAction = btnStart;
        this.resultEl = resultEl;
        this.resultSection = resultSection;
        this.indicator = indicator;

        if (btnStats instanceof HTMLButtonElement) {
            btnStats.onclick = () => {
                const panel = document.getElementById("stats-panel");
                if (!panel) {
                    return;
                }

                const expanded = btnStats.getAttribute("aria-expanded") === "true";
                btnStats.setAttribute("aria-expanded", String(!expanded));
                panel.hidden = expanded;
            };
        }

        this.setPhase("ready");
    }

    get challengeUrl(): string {
        return this.container.dataset.challengeUrl || "/challenge";
    }

    onStart(handler: () => void) {
        this.startHandler = handler;
        this.syncActionButton();
    }

    onCancel(handler: () => void) {
        this.cancelHandler = handler;
        this.syncActionButton();
    }

    setPhase(phase: ChallengePhase) {
        this.phase = phase;

        switch (phase) {
            case "ready":
                this.updateStatus("Ready");
                this.setIndicator("idle");
                break;
            case "fetching":
                this.updateStatus("Fetching challenge...");
                this.setIndicator("working");
                break;
            case "solving":
                this.updateStatus("Solving...");
                this.setIndicator("working");
                break;
            case "validating":
                this.updateStatus("Validating...");
                this.setIndicator("working");
                break;
        }

        this.syncActionButton();
    }

    showError(message: string) {
        this.phase = "ready";
        this.updateStatus("Error");
        this.setIndicator("error");
        this.syncActionButton();
        this.resultEl.textContent = message;
        this.resultEl.style.color = "#c0392b";
        this.resultSection.hidden = false;
    }

    showSuccess(message: string) {
        this.phase = "ready";
        this.updateStatus("Solved");
        this.setIndicator("success");
        this.syncActionButton();
        this.resultEl.textContent = message;
        this.resultEl.style.color = "#1f9d63";
        this.resultSection.hidden = false;
    }

    clearResult() {
        this.resultEl.textContent = "";
        this.resultEl.style.color = "";
        this.resultSection.hidden = true;
    }

    updateProgress(progress: Progress) {
        this.hpsEl.textContent = `Hashes/sec: ${progress.hashesPerSec || 0}`;
        this.attemptsEl.textContent = `Attempts: ${progress.attempts}`;
    }

    private updateStatus(text: string) {
        this.statusEl.textContent = text;
    }

    private setIndicator(state: IndicatorState) {
        this.indicator.dataset.state = state;
    }

    private syncActionButton() {
        this.btnAction.classList.remove("is-cancel");

        switch (this.phase) {
            case "ready":
                this.btnAction.textContent = "Solve Challenge";
                this.btnAction.disabled = false;
                this.btnAction.onclick = () => this.startHandler?.();
                break;
            case "fetching":
                this.btnAction.textContent = "Fetching…";
                this.btnAction.disabled = true;
                this.btnAction.onclick = null;
                break;
            case "solving":
                this.btnAction.textContent = "Cancel";
                this.btnAction.disabled = false;
                this.btnAction.classList.add("is-cancel");
                this.btnAction.onclick = () => this.cancelHandler?.();
                break;
            case "validating":
                this.btnAction.textContent = "Validating…";
                this.btnAction.disabled = true;
                this.btnAction.onclick = null;
                break;
        }
    }
}
