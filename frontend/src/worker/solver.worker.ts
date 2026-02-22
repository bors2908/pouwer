import { sha256 } from "js-sha256";
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
  const batchSize = 5000;

  while (running) {
    for (let i = 0; i < batchSize; i++) {
      attempts++;
      const solution = counter.toString();
      const solutionBytes = utf8ToBytes(solution);
      const data = concatUint8(nonceBytes, solutionBytes);

      const hashBytes = new Uint8Array(sha256.array(data));

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

    // Since we are no longer using await inside the loop, we should yield
    // to allow 'cancel' message to be processed.
    await new Promise(r => setTimeout(r, 0));
  }
}

async function solveCrypto(challenge: Challenge) {
  const headerPrefix = hexToBytes(challenge.headerPrefixHex!);
  const targetHex = challenge.targetHex!;
  const targetBI = BigInt("0x" + targetHex);
  const nonceStart = BigInt(challenge.nonceStart || 0);
  const nonceEnd = BigInt(challenge.nonceEnd || 0xffffffff);
  
  const startTime = Date.now();
  let attempts = 0;
  let lastProgressTime = startTime;
  let nonce = nonceStart;

  const batchSize = 10000;
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

      const hash1 = sha256.array(header);
      const hash2 = sha256.array(hash1);
      const hashBytes = new Uint8Array(hash2);
      
      // Bitcoin hash is compared numerically. 
      // The hash we get from sha256 is Big Endian, but Bitcoin compares it as Little Endian for the RPC,
      // HOWEVER, the standard says we compare the 256-bit integer.
      // In the previous code we were doing: bytesToHex(hashBytes.reverse()) <= targetHex
      // If we want to compare BigInts, we need to make sure we treat hashBytes correctly.
      // Bitcoin hashes are typically shown reversed.
      
      // Let's stick to the numerical comparison:
      // We need to convert hashBytes (which is reversed in Bitcoin terms) to BigInt.
      // If currentHashHex was bytesToHex(hashBytes.reverse()), then:
      const hashBI = BigInt("0x" + bytesToHex(new Uint8Array(hashBytes).reverse()));

      if (hashBI <= targetBI) {
        const durationMs = Date.now() - startTime;
        self.postMessage({
          type: "solved",
          solution: nonce.toString(),
          nonce: Number(nonce),
          hashHex: bytesToHex(new Uint8Array(hashBytes).reverse()),
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
    
    // Yield
    await new Promise(r => setTimeout(r, 0));
  }

  if (nonce > nonceEnd) {
     self.postMessage({ type: "stopped", reason: "Range exhausted" } as WorkerOutMessage);
  } else {
     self.postMessage({ type: "stopped", reason: "Cancelled" } as WorkerOutMessage);
  }
}
