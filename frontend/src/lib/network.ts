import { Challenge, SolveRequest, ValidateResponse } from "./types";

export class NetworkClient {
  baseUrl: string;
  timeoutMs: number;

  constructor(baseUrl: string = "http://localhost:8081", timeoutMs: number = 10000) {
    this.baseUrl = baseUrl;
    this.timeoutMs = timeoutMs;
  }

  async getChallenge(type: "crypto" | "pow" = "crypto"): Promise<Challenge> {
    const workerId = Math.floor(Math.random() * 100);
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), this.timeoutMs);
    const url = type === "crypto" 
      ? `${this.baseUrl}/challenge-crypto?worker=${workerId}`
      : `${this.baseUrl}/challenge`;

    try {
      const response = await fetch(url, {
        signal: controller.signal,
      });

      clearTimeout(id);

      if (!response.ok) {
        throw new Error(`Failed to fetch challenge: HTTP ${response.status}`);
      }

      return await response.json();
    } catch (e: any) {
      clearTimeout(id);
      throw e;
    }
  }

  async postValidateCrypto(req: any): Promise<ValidateResponse> {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), this.timeoutMs);

    try {
      const response = await fetch(`${this.baseUrl}/validate-crypto`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(req),
        signal: controller.signal,
      });

      clearTimeout(id);

      if (response.ok) {
        const result = await response.json();
        if (result.status === "ok") {
          return { ok: true };
        } else {
          return { ok: false, reason: "Validation failed" };
        }
      } else {
        const text = await response.text();
        return { ok: false, reason: `HTTP ${response.status}: ${text}` };
      }
    } catch (e: any) {
      clearTimeout(id);
      return { ok: false, reason: e.message || "Network error" };
    }
  }

  async postValidate(req: SolveRequest): Promise<ValidateResponse> {
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
        return { ok: true };
      } else {
        const text = await response.text();
        return { ok: false, reason: `HTTP ${response.status}: ${text}` };
      }
    } catch (e: any) {
      clearTimeout(id);
      return { ok: false, reason: e.message || "Network error" };
    }
  }
}
