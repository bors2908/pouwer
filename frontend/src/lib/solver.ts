import { Challenge, ISolver, Progress, SolveResult, WorkerOutMessage } from "./types";
import { bytesToHex, hexToBytes, nowMs, sleep } from "./utils";

export class MainThreadSolver implements ISolver {
  private running = false;

  async start(challenge: Challenge, onProgress?: (stats: Progress) => void): Promise<SolveResult> {
    this.running = true;
    const nonceBytes = hexToBytes(challenge.payload.dataHex);
    const targetBI = BigInt("0x" + challenge.payload.targetHex);
    const startTime = nowMs();
    let attempts = 0;
    let lastProgressTime = startTime;
    let nonce = BigInt(challenge.payload.nonceRange.start);
    const nonceEnd = BigInt(challenge.payload.nonceRange.end);
    const offset = challenge.payload.nonceOffset;
    const isLE = challenge.payload.nonceIsLE;

    const batchSize = 10;
    const buffer = new Uint8Array(Math.max(nonceBytes.length, offset + 4));
    buffer.set(nonceBytes);

    while (this.running && nonce <= nonceEnd) {
      for (let i = 0; i < batchSize && nonce <= nonceEnd; i++) {
        attempts++;
        const n = Number(nonce & 0xffffffffn);
        if (isLE) {
          buffer[offset] = n & 0xff;
          buffer[offset + 1] = (n >> 8) & 0xff;
          buffer[offset + 2] = (n >> 16) & 0xff;
          buffer[offset + 3] = (n >> 24) & 0xff;
        } else {
          buffer[offset] = (n >> 24) & 0xff;
          buffer[offset + 1] = (n >> 16) & 0xff;
          buffer[offset + 2] = (n >> 8) & 0xff;
          buffer[offset + 3] = n & 0xff;
        }

        const hashBuffer1 = await crypto.subtle.digest("SHA-256", buffer.buffer as ArrayBuffer);
        const hashBuffer2 = await crypto.subtle.digest("SHA-256", hashBuffer1);
        const hashBytes = new Uint8Array(hashBuffer2);
        
        const reversedHash = new Uint8Array(hashBytes).reverse();
        const hashBI = BigInt("0x" + bytesToHex(reversedHash));

        if (hashBI <= targetBI) {
          this.running = false;
          const hashHex = bytesToHex(reversedHash);
          const durationMs = nowMs() - startTime;
          return {
            solution: nonce.toString(),
            hashHex,
            attempts,
            durationMs,
            payload: {
              dataHex: challenge.payload.dataHex,
              nonce: Number(nonce),
              hashHex,
              durationMs,
              attempts,
            }
          } as any;
        }
        nonce++;
      }

      const currentTime = nowMs();
      if (currentTime - lastProgressTime >= 500) {
        if (onProgress) {
          const elapsedMs = currentTime - startTime;
          onProgress({
            attempts,
            elapsedMs,
            hashesPerSec: Math.floor((attempts * 1000) / elapsedMs),
          });
        }
        lastProgressTime = currentTime;
        // Yield to UI
        await sleep(0);
      }
    }
    throw new Error("Cancelled");
  }

  cancel(): void {
    this.running = false;
  }
}

export class WebWorkerSolver implements ISolver {
  private worker: Worker | null = null;

  async start(challenge: Challenge, onProgress?: (stats: Progress) => void): Promise<SolveResult> {
    return new Promise((resolve, reject) => {
      // Assuming solver.worker.ts is in the same directory or accessible
      this.worker = new Worker(new URL("../worker/solver.worker.ts", import.meta.url), { type: "module" });

      this.worker.onmessage = (e: MessageEvent<WorkerOutMessage>) => {
        const msg = e.data;
        if (msg.type === "progress" && onProgress) {
          onProgress(msg);
        } else if (msg.type === "solved") {
          this.worker?.terminate();
          resolve(msg);
        } else if (msg.type === "stopped") {
          this.worker?.terminate();
          reject(new Error(msg.reason || "Stopped"));
        }
      };

      this.worker.onerror = (e) => {
        this.worker?.terminate();
        reject(e);
      };

      this.worker.postMessage({ type: "init", challenge });
      this.worker.postMessage({ type: "start" });
    });
  }

  cancel(): void {
    if (this.worker) {
      this.worker.postMessage({ type: "cancel" });
      this.worker.terminate();
      this.worker = null;
    }
  }
}
