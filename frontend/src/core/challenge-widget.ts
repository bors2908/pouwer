import {ISolver, JobType, Progress, ResultMessage, SolveResult, Task} from "../contracts";
import {NetworkClient} from "./network";
import {PayloadModule} from "./payload-module";
import {WebWorkerSolver} from "./solver";

export interface ChallengeWidgetConfig {
    modules: readonly PayloadModule[];
    defaultJobType?: JobType;
}

type ChallengeTypeOption = {
    value: JobType;
    label: string;
};

class ChallengeUI {
    task: Task | null = null;

    network: NetworkClient;

    solver: ISolver;

    private readonly modulesByType: Map<JobType, PayloadModule>;
    private readonly defaultJobType?: JobType;

    uiElements: {
        container: HTMLElement;
        form: HTMLFormElement;
        statusEl: HTMLElement;
        hpsEl: HTMLElement;
        attemptsEl: HTMLElement;
        btnStart: HTMLButtonElement;
        btnCancel: HTMLButtonElement;
        btnType: HTMLSelectElement | null;
        resultEl: HTMLElement;
        hiddenResponse: HTMLInputElement;
    };

    constructor(config: ChallengeWidgetConfig) {
        if (config.modules.length === 0) {
            throw new Error("At least one payload module must be configured.");
        }

        this.defaultJobType = config.defaultJobType;
        this.modulesByType = new Map(
            config.modules
                .filter((module) => module.enabled !== false)
                .map((module) => [module.jobType, module] as const)
        );

        if (this.modulesByType.size === 0) {
            throw new Error("All payload modules are disabled.");
        }

        this.network = new NetworkClient();

        if (typeof Worker !== "undefined") {
            this.solver = new WebWorkerSolver(this.createScriptMap());
        } else {
            throw new Error("Workers are not supported in this browser. Main thread solver is not implemented yet.");
        }

        const container = document.getElementById("captcha");
        const form = document.getElementById("captcha-form");
        const hiddenResponse = document.getElementById("captcha-response");

        if (!container || !(form instanceof HTMLFormElement) || !(hiddenResponse instanceof HTMLInputElement)) {
            throw new Error("Captcha widget markup is missing.");
        }

        const btnType = document.getElementById("challengeType");

        this.uiElements = {
            container,
            form,
            statusEl: document.getElementById("status")!,
            hpsEl: document.getElementById("hps")!,
            attemptsEl: document.getElementById("attempts")!,
            btnStart: document.getElementById("btnStart") as HTMLButtonElement,
            btnCancel: document.getElementById("btnCancel") as HTMLButtonElement,
            btnType: btnType instanceof HTMLSelectElement ? btnType : null,
            resultEl: document.getElementById("result")!,
            hiddenResponse,
        };

        this.populateChallengeTypeOptions();
        this.initEvents();
    }

    private createScriptMap(): Record<JobType, new () => Worker> {
        return Array.from(this.modulesByType.values()).reduce(
            (acc, module) => {
                acc[module.jobType] = module.workerFactory;
                return acc;
            },
            {} as Record<JobType, new () => Worker>
        );
    }

    private getChallengeTypeOptions(): ChallengeTypeOption[] {
        return Array.from(this.modulesByType.values()).map((module) => ({
            value: module.jobType,
            label: module.label,
        }));
    }

    private populateChallengeTypeOptions() {
        const select = this.uiElements.btnType;
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

    private resolveSelectedJobType(): JobType {
        const selectedType = this.uiElements.btnType?.value as JobType | undefined;
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

    private async startSolving() {
        const type = this.resolveSelectedJobType();
        this.uiElements.btnStart.disabled = true;
        this.uiElements.btnCancel.disabled = true;
        if (this.uiElements.btnType) {
            this.uiElements.btnType.disabled = true;
        }
        this.uiElements.resultEl.textContent = "";
        this.updateStatus("Fetching challenge...");

        try {
            this.task = await this.network.getChallenge(type);

            if (this.task.expiresAt && this.task.expiresAt < Date.now()) {
                this.updateStatus("Challenge expired");
                this.uiElements.btnStart.disabled = false;
                if (this.uiElements.btnType) {
                    this.uiElements.btnType.disabled = false;
                }
                return;
            }

            this.updateStatus("Solving...");
            this.uiElements.btnCancel.disabled = false;

            const result = await this.solver.start(this.task, (progress) => this.updateProgress(progress));
            this.updateStatus("Solved! Validating...");
            this.updateProgress({
                attempts: result.attempts,
                elapsedMs: result.durationMs,
                hashesPerSec: Math.floor((result.attempts * 1000) / result.durationMs),
            });

            const resultMessage = this.getResultMessage(this.task, result);
            this.deliverResultToPlugin(resultMessage);

            this.updateStatus("Submitted to plugin");
            this.uiElements.resultEl.textContent = "Result handed back to Traefik";
            this.uiElements.resultEl.style.color = "green";
        } catch (error) {
            if (error instanceof Error && error.message !== "Cancelled") {
                this.updateStatus("Error");
                this.uiElements.resultEl.textContent = `Error: ${error.message}`;
                this.uiElements.resultEl.style.color = "red";
            } else {
                this.updateStatus("Ready");
            }
        } finally {
            this.uiElements.btnStart.disabled = false;
            this.uiElements.btnCancel.disabled = true;
            if (this.uiElements.btnType) {
                this.uiElements.btnType.disabled = false;
            }
        }
    }

    private getResultMessage(task: Task, result: SolveResult): ResultMessage {
        return {
            jobId: task.jobId,
            payload: result.payload,
            leaseHmac: task.leaseHmac,
            attempts: result.attempts,
            durationMs: result.durationMs,
        };
    }

    private deliverResultToPlugin(resultMessage: ResultMessage) {
        const responseField = this.uiElements.container.dataset.responseField || "response";

        const serialized = JSON.stringify(resultMessage);
        this.uiElements.hiddenResponse.name = responseField;
        this.uiElements.hiddenResponse.value = serialized;

        const callbackName = this.uiElements.container.dataset.callback || "captchaCallback";
        const callback = Reflect.get(window, callbackName);

        if (typeof callback === "function") {
            callback(serialized);
            return;
        }

        this.uiElements.form.submit();
    }

    private cancelSolving() {
        this.solver.cancel();
        this.updateStatus("Ready");
        this.uiElements.btnStart.disabled = false;
        this.uiElements.btnCancel.disabled = true;
        if (this.uiElements.btnType) {
            this.uiElements.btnType.disabled = false;
        }
    }
}

export const bootstrapChallengeWidget = (config: ChallengeWidgetConfig) => {
    const bootstrap = () => {
        new ChallengeUI(config);
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", bootstrap, {once: true});
    } else {
        bootstrap();
    }
};
