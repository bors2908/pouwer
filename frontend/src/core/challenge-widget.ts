import {BaseTask, ISolver, JobType, ResultMessage, SolveResult} from "./models";
import {ChallengeUI} from "./challenge-ui";
import {NetworkClient} from "./network";
import {PayloadModule} from "./payload-module";
import {deliverResultToPlugin} from "./result-delivery";
import {WebWorkerSolver} from "./solver";

export interface ChallengeWidgetConfig {
    modules: readonly PayloadModule[];
    defaultJobType?: JobType;
}

class ChallengeController {
    task: BaseTask | null = null;

    private readonly network: NetworkClient<BaseTask>;
    private readonly solver: ISolver<BaseTask, unknown>;
    private readonly ui: ChallengeUI;
    private readonly modulesByType: Map<JobType, PayloadModule>;
    private readonly defaultJobType?: JobType;

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

        if (typeof Worker !== "undefined") {
            this.solver = new WebWorkerSolver(this.createScriptMap());
        } else {
            throw new Error("Workers are not supported in this browser. Main thread solver is not implemented yet.");
        }

        this.ui = new ChallengeUI(this.modulesByType, this.defaultJobType);
        this.network = new NetworkClient({challengeUrl: this.ui.challengeUrl});
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

    private initEvents() {
        this.ui.onStart(() => void this.startSolving());
        this.ui.onCancel(() => this.cancelSolving());
    }

    private resolveSelectedJobType(): JobType {
        const selectedType = this.ui.getSelectedJobType();
        if (!this.modulesByType.has(selectedType)) {
            throw new Error("No payload module selected.");
        }
        return selectedType;
    }

    private async startSolving() {
        const type = this.resolveSelectedJobType();
        this.ui.clearResult();
        this.ui.setPhase("fetching");

        try {
            this.task = await this.network.getChallenge(type);

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
            deliverResultToPlugin({
                container: this.ui.container,
                form: this.ui.form,
                hiddenResponse: this.ui.hiddenResponse,
            }, resultMessage);
            this.ui.showSuccess("Result handed back to Traefik");
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
