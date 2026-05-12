import type {BaseTask, ISolver, PayloadBinding, ResultMessage, SolveResult} from "@pouwer/core-contracts";
import {WebWorkerSolver} from "@pouwer/worker-runtime";
import {ChallengeUI} from "./challenge-ui.js";
import {NetworkClient} from "./network.js";

type ResultHandler = (result: ResultMessage<unknown>, ui: ChallengeUI) => Promise<void> | void;

export interface ChallengeWidgetConfig {
    payload: PayloadBinding;
    onSolved?: ResultHandler;
    successMessage?: string;
}

class ChallengeController {
    task: BaseTask | null = null;

    private readonly network: NetworkClient<BaseTask>;
    private readonly solver: ISolver<BaseTask, unknown>;
    private readonly ui: ChallengeUI;
    private readonly payload: PayloadBinding;
    private readonly onSolved?: ResultHandler;
    private readonly successMessage: string;

    constructor(config: ChallengeWidgetConfig) {
        this.payload = config.payload;
        this.onSolved = config.onSolved;
        this.successMessage = config.successMessage || "Challenge solved";

        if (typeof Worker !== "undefined") {
            this.solver = new WebWorkerSolver(this.payload.workerFactory);
        } else {
            throw new Error("Workers are not supported in this browser. Main thread solver is not implemented yet.");
        }

        this.ui = new ChallengeUI();
        this.network = new NetworkClient({challengeUrl: this.ui.challengeUrl});
        this.initEvents();
    }

    private initEvents() {
        this.ui.onStart(() => void this.startSolving());
        this.ui.onCancel(() => this.cancelSolving());
    }

    private async startSolving() {
        this.ui.clearResult();
        this.ui.setPhase("fetching");

        try {
            this.task = await this.network.getChallenge(this.payload.jobType);

            if (this.task.expiresAt && this.task.expiresAt < Date.now()) {
                this.ui.showError("Challenge expired");
                return;
            }

            this.ui.setPhase("solving");

            const result = await this.solver.start(this.task, (progress) => this.ui.updateProgress(progress));
            this.ui.setPhase("validating");
            this.ui.updateProgress({
                attempts: result.attempts,
                elapsedMs: result.durationMs,
                hashesPerSec: Math.floor((result.attempts * 1000) / result.durationMs),
            });

            const resultMessage = this.getResultMessage(this.task, result);
            await this.onSolved?.(resultMessage, this.ui);
            this.ui.showSuccess(this.successMessage);
        } catch (error) {
            if (error instanceof Error && error.message !== "Cancelled") {
                this.ui.showError(`Error: ${error.message}`);
            } else {
                this.ui.setPhase("ready");
            }
        }
    }

    private getResultMessage(task: BaseTask, result: SolveResult<unknown>): ResultMessage<unknown> {
        return {
            jobId: task.jobId,
            payload: result.payload,
            leaseHmac: task.leaseHmac,
            attempts: result.attempts,
            durationMs: result.durationMs,
        };
    }

    private cancelSolving() {
        this.solver.cancel();
        this.ui.setPhase("ready");
    }
}

export const bootstrapChallengeWidget = (config: ChallengeWidgetConfig) => {
    const bootstrap = () => {
        new ChallengeController(config);
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", bootstrap, {once: true});
    } else {
        bootstrap();
    }
};
