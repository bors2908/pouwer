import type {BaseTask, JobType} from "@pouwer/core-contracts";

type NetworkClientConfig = {
    challengeUrl?: string;
    timeoutMs?: number;
};

function appendQuery(url: string, query: Record<string, string>): string {
    const queryString = new URLSearchParams(query).toString();
    const separator = url.includes("?") ? "&" : "?";
    return `${url}${separator}${queryString}`;
}

export class NetworkClient<TTask extends BaseTask = BaseTask> {
    private readonly challengeUrl: string;
    private readonly timeoutMs: number;

    constructor(config: NetworkClientConfig = {}) {
        this.challengeUrl = config.challengeUrl || "/challenge";
        this.timeoutMs = config.timeoutMs ?? 10000;
    }

    async getChallenge(type: JobType): Promise<TTask> {
        const workerId = Math.floor(Math.random() * 100);
        const controller = new AbortController();
        const id = setTimeout(() => controller.abort(), this.timeoutMs);
        const url = appendQuery(this.challengeUrl, {
            workerId: String(workerId),
            jobType: type,
        });

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
}
