import { BaseTask, JobType, ResultMessage, ValidateResponse } from "./models";

export class NetworkClient<TTask extends BaseTask = BaseTask> {
    baseUrl: string;
    timeoutMs: number;

    constructor(baseUrl: string = "http://localhost:8082", timeoutMs: number = 10000) {
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
    }

    async getChallenge(type: JobType): Promise<TTask> {
        const workerId = Math.floor(Math.random() * 100);
        const controller = new AbortController();
        const id = setTimeout(() => controller.abort(), this.timeoutMs);
        const url = `${this.baseUrl}/challenge?workerId=${workerId}&jobType=${type}`;

        try {
            const response = await fetch(url, {
                signal: controller.signal,
            });

            clearTimeout(id);

            if (!response.ok) {
                throw new Error(`Failed to fetch challenge: HTTP ${response.status}`);
            }

            return await response.json() as TTask;
        } catch (error) {
            clearTimeout(id);
            throw error;
        }
    }

    async postValidate(req: ResultMessage): Promise<ValidateResponse> {
        const controller = new AbortController();
        const id = setTimeout(() => controller.abort(), this.timeoutMs);

        try {
            const response = await fetch(`${this.baseUrl}/validate`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(req),
                signal: controller.signal,
            });

            clearTimeout(id);

            if (response.ok) {
                return {ok: true};
            }

            const text = await response.text();
            let reason = text;
            try {
                const body = JSON.parse(text);
                reason = body.reason || body.status || text;
            } catch {
            }
            return {ok: false, reason: `HTTP ${response.status}: ${reason}`};
        } catch (error) {
            const reason = error instanceof Error ? error.message : "Network error";
            clearTimeout(id);
            return {ok: false, reason};
        }
    }
}
