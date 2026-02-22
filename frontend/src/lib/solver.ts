import { Challenge, ISolver, Progress, SolveResult, WorkerOutMessage } from "./types";
import { bytesToHex, concatUint8, leadingZeroBits, nowMs, sleep, utf8ToBytes } from "./utils";

export class MainThreadSolver implements ISolver {
  private running = false;

  async start(challenge: Challenge, onProgress?: (stats: Progress) => void): Promise<SolveResult> {
    this.running = true;
    const nonceBytes = utf8ToBytes(challenge.nonce);
    const difficulty = challenge.difficulty;
    const startTime = nowMs();
    let attempts = 0;
    let lastProgressTime = startTime;
    let counter = BigInt(Math.floor(Math.random() * 0xffffffff));

    const batchSize = 100;

    while (this.running) {
      for (let i = 0; i < batchSize; i++) {
        attempts++;
        const solution = counter.toString();
        const data = concatUint8(nonceBytes, utf8ToBytes(solution));

        const hashBuffer = await crypto.subtle.digest("SHA-256", data);
        const hashBytes = new Uint8Array(hashBuffer);

        if (leadingZeroBits(hashBytes) >= difficulty) {
          this.running = false;
          return {
            solution,
            hashHex: bytesToHex(hashBytes),
            attempts,
            durationMs: nowMs() - startTime,
          };
        }
        counter++;
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
      // Assuming solver.worker.js is in the same directory or accessible
      this.worker = new Worker(new URL("../worker/solver.worker.js", import.meta.url));

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
