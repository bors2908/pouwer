import { Challenge, WorkerInMessage, WorkerOutMessage } from "../lib/types";
import { bytesToHex, concatUint8, leadingZeroBits, nowMs, utf8ToBytes } from "../lib/utils";

let currentChallenge: Challenge | null = null;
let running = false;

self.onmessage = (e: MessageEvent<WorkerInMessage>) => {
  const msg = e.data;
  switch (msg.type) {
    case "init":
      currentChallenge = msg.challenge;
      break;
    case "start":
      if (currentChallenge) {
        running = true;
        solve(currentChallenge);
      }
      break;
    case "cancel":
      running = false;
      break;
  }
};

async function solve(challenge: Challenge) {
  const nonceBytes = utf8ToBytes(challenge.nonce);
  const difficulty = challenge.difficulty;
  const startTime = nowMs();
  let attempts = 0;
  let lastProgressTime = startTime;

  // Use a random starting point for the solution
  let counter = BigInt(Math.floor(Math.random() * 0xffffffff));
  const batchSize = 1000;

  while (running) {
    for (let i = 0; i < batchSize; i++) {
      attempts++;
      const solution = counter.toString();
      const solutionBytes = utf8ToBytes(solution);
      const data = concatUint8(nonceBytes, solutionBytes);

      // WebCrypto is async. crypto.subtle.digest is available in workers.
      const hashBuffer = await crypto.subtle.digest("SHA-256", data.buffer as ArrayBuffer);
      const hashBytes = new Uint8Array(hashBuffer);

      if (leadingZeroBits(hashBytes) >= difficulty) {
        const durationMs = nowMs() - startTime;
        const hashHex = bytesToHex(hashBytes);
        self.postMessage({
          type: "solved",
          solution,
          hashHex,
          attempts,
          durationMs,
        } as WorkerOutMessage);
        running = false;
        return;
      }
      counter++;
    }

    const currentTime = nowMs();
    if (currentTime - lastProgressTime >= 500) {
      const elapsedMs = currentTime - startTime;
      const hps = Math.floor((attempts * 1000) / elapsedMs);
      self.postMessage({
        type: "progress",
        attempts,
        elapsedMs,
        hashesPerSec: hps,
      } as WorkerOutMessage);
      lastProgressTime = currentTime;
    }

    // Yield to the event loop if needed, though subtle.digest is already yielding.
    // In workers, we don't necessarily need to sleep(0) if we are doing async work.
  }

  self.postMessage({ type: "stopped", reason: "Cancelled" } as WorkerOutMessage);
}
