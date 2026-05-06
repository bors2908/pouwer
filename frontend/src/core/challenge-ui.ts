import {JobType, Progress} from "./models";
import {PayloadModule} from "./payload-module";

type ChallengeTypeOption = {
    value: JobType;
    label: string;
};

type ChallengePhase = "ready" | "fetching" | "solving" | "validating";

export class ChallengeUI {
    readonly container: HTMLElement;
    readonly form: HTMLFormElement;
    readonly hiddenResponse: HTMLInputElement;

    private readonly statusEl: HTMLElement;
    private readonly hpsEl: HTMLElement;
    private readonly attemptsEl: HTMLElement;
    private readonly btnStart: HTMLButtonElement;
    private readonly btnCancel: HTMLButtonElement;
    private readonly btnType: HTMLSelectElement | null;
    private readonly resultEl: HTMLElement;
    private readonly modulesByType: Map<JobType, PayloadModule>;
    private readonly defaultJobType?: JobType;

    constructor(modulesByType: Map<JobType, PayloadModule>, defaultJobType?: JobType) {
        this.modulesByType = modulesByType;
        this.defaultJobType = defaultJobType;

        const container = document.getElementById("captcha");
        const form = document.getElementById("captcha-form");
        const hiddenResponse = document.getElementById("captcha-response");

        if (!container || !(form instanceof HTMLFormElement) || !(hiddenResponse instanceof HTMLInputElement)) {
            throw new Error("Captcha widget markup is missing.");
        }

        const statusEl = document.getElementById("status");
        const hpsEl = document.getElementById("hps");
        const attemptsEl = document.getElementById("attempts");
        const btnStart = document.getElementById("btnStart");
        const btnCancel = document.getElementById("btnCancel");
        const resultEl = document.getElementById("result");
        const btnType = document.getElementById("challengeType");

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
        this.form = form;
        this.hiddenResponse = hiddenResponse;
        this.statusEl = statusEl;
        this.hpsEl = hpsEl;
        this.attemptsEl = attemptsEl;
        this.btnStart = btnStart;
        this.btnCancel = btnCancel;
        this.resultEl = resultEl;
        this.btnType = btnType instanceof HTMLSelectElement ? btnType : null;

        this.populateChallengeTypeOptions();
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

    getSelectedJobType(): JobType {
        const selectedType = this.btnType?.value as JobType | undefined;
        if (selectedType && this.modulesByType.has(selectedType)) {
            return selectedType;
        }

        if (this.defaultJobType && this.modulesByType.has(this.defaultJobType)) {
            return this.defaultJobType;
        }

        const fallback = this.modulesByType.keys().next().value as JobType | undefined;
        if (!fallback) {
            throw new Error("No payload module selected.");
        }
        return fallback;
    }

    setPhase(phase: ChallengePhase) {
        switch (phase) {
            case "ready":
                this.updateStatus("Ready");
                this.setControlState(false, true, false);
                break;
            case "fetching":
                this.updateStatus("Fetching challenge...");
                this.setControlState(true, true, true);
                break;
            case "solving":
                this.updateStatus("Solving...");
                this.setControlState(true, false, true);
                break;
            case "validating":
                this.updateStatus("Solved! Validating...");
                this.setControlState(true, true, true);
                break;
        }
    }

    showError(message: string) {
        this.updateStatus("Error");
        this.setControlState(false, true, false);
        this.resultEl.textContent = message;
        this.resultEl.style.color = "red";
    }

    showSuccess(message: string) {
        this.updateStatus("Submitted to plugin");
        this.setControlState(false, true, false);
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

    private setControlState(startDisabled: boolean, cancelDisabled: boolean, typeDisabled: boolean) {
        this.btnStart.disabled = startDisabled;
        this.btnCancel.disabled = cancelDisabled;
        if (this.btnType) {
            this.btnType.disabled = typeDisabled;
        }
    }

    private getChallengeTypeOptions(): ChallengeTypeOption[] {
        return Array.from(this.modulesByType.values()).map((module) => ({
            value: module.jobType,
            label: module.label,
        }));
    }

    private populateChallengeTypeOptions() {
        const select = this.btnType;
        if (!select) {
            return;
        }

        const options = this.getChallengeTypeOptions();
        select.innerHTML = "";

        for (const optionConfig of options) {
            const optionEl = document.createElement("option");
            optionEl.value = optionConfig.value;
            optionEl.textContent = optionConfig.label;
            select.append(optionEl);
        }

        const selectedType = this.defaultJobType && this.modulesByType.has(this.defaultJobType)
            ? this.defaultJobType
            : options[0]?.value;

        if (selectedType) {
            select.value = selectedType;
        }

        if (options.length <= 1) {
            select.disabled = true;
            const parent = select.parentElement;
            if (parent) {
                parent.style.display = "none";
            }
        }
    }
}
