import { Challenge, WorkerInMessage, WorkerOutMessage } from "../lib/types";
import { bytesToHex, concatUint8, hexToBytes, leadingZeroBits, nowMs, utf8ToBytes } from "../lib/utils";

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
  if (challenge.jobId && challenge.headerPrefixHex && challenge.targetHex) {
    return solveCrypto(challenge);
  }

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

async function solveCrypto(challenge: Challenge) {
  const headerPrefix = hexToBytes(challenge.headerPrefixHex!);
  const targetHex = challenge.targetHex!;
  const nonceStart = BigInt(challenge.nonceStart || 0);
  const nonceEnd = BigInt(challenge.nonceEnd || 0xffffffff);
  
  const startTime = Date.now();
  let attempts = 0;
  let lastProgressTime = startTime;
  let nonce = nonceStart;

  const batchSize = 1000;
  const header = new Uint8Array(80);
  header.set(headerPrefix);

  while (running && nonce <= nonceEnd) {
    for (let i = 0; i < batchSize && nonce <= nonceEnd; i++) {
      attempts++;
      
      // uint32 LE
      header[76] = Number(nonce & 0xffn);
      header[77] = Number((nonce >> 8n) & 0xffn);
      header[78] = Number((nonce >> 16n) & 0xffn);
      header[79] = Number((nonce >> 24n) & 0xffn);

      const hash1 = await crypto.subtle.digest("SHA-256", header);
      const hash2 = await crypto.subtle.digest("SHA-256", hash1);
      const hashBytes = new Uint8Array(hash2);
      
      // Bitcoin hash is displayed reversed (LE)
      const currentHashHex = bytesToHex(hashBytes.reverse());

      if (currentHashHex.padStart(64, "0") <= targetHex.padStart(64, "0")) {
        const durationMs = Date.now() - startTime;
        self.postMessage({
          type: "solved",
          solution: nonce.toString(),
          nonce: Number(nonce),
          hashHex: currentHashHex,
          attempts,
          durationMs,
        } as WorkerOutMessage);
        running = false;
        return;
      }
      nonce++;
    }

    const currentTime = Date.now();
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
  }

  if (nonce > nonceEnd) {
     self.postMessage({ type: "stopped", reason: "Range exhausted" } as WorkerOutMessage);
  } else {
     self.postMessage({ type: "stopped", reason: "Cancelled" } as WorkerOutMessage);
  }
}

self.postMessage({ type: "stopped", reason: "Cancelled" } as WorkerOutMessage);
}
