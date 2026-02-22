import { NetworkClient } from "../lib/network";
import { MainThreadSolver, WebWorkerSolver } from "../lib/solver";
import { Challenge, ISolver, Progress } from "../lib/types";

declare global {
  interface Window {
    __CHALLENGE__?: Challenge;
  }
}

class ChallengeUI {
  challenge: Challenge | null = null;
  network: NetworkClient;
  solver: ISolver;
  uiElements: {
    statusEl: HTMLElement;
    hpsEl: HTMLElement;
    attemptsEl: HTMLElement;
    btnStart: HTMLButtonElement;
    btnCancel: HTMLButtonElement;
    resultEl: HTMLElement;
  };

  constructor() {
    this.network = new NetworkClient();

    // Prefer WebWorker if supported
    if (typeof Worker !== "undefined") {
      this.solver = new WebWorkerSolver();
    } else {
      this.solver = new MainThreadSolver();
      console.warn("WebWorkers not supported, using main thread.");
    }

    this.uiElements = {
      statusEl: document.getElementById("status")!,
      hpsEl: document.getElementById("hps")!,
      attemptsEl: document.getElementById("attempts")!,
      btnStart: document.getElementById("btnStart") as HTMLButtonElement,
      btnCancel: document.getElementById("btnCancel") as HTMLButtonElement,
      resultEl: document.getElementById("result")!,
    };

    this.initEvents();
  }

  private initEvents() {
    this.uiElements.btnStart.onclick = () => this.startSolving();
    this.uiElements.btnCancel.onclick = () => this.cancelSolving();
  }

  private updateStatus(text: string) {
    this.uiElements.statusEl.textContent = `Status: ${text}`;
  }

  private updateProgress(progress: Progress) {
    this.uiElements.hpsEl.textContent = `Hashes/sec: ${progress.hashesPerSec || 0}`;
    this.uiElements.attemptsEl.textContent = `Attempts: ${progress.attempts}`;
  }

  private async startSolving() {
    this.uiElements.btnStart.disabled = true;
    this.uiElements.btnCancel.disabled = true; // Disabled during fetch
    this.uiElements.resultEl.textContent = "";
    this.updateStatus("Fetching challenge...");

    try {
      this.challenge = await this.network.getChallenge();

      if (this.challenge.expiresAt && this.challenge.expiresAt < Date.now()) {
        this.updateStatus("Challenge expired");
        this.uiElements.btnStart.disabled = false;
        return;
      }

      this.updateStatus("Solving...");
      this.uiElements.btnCancel.disabled = false;

      const result = await this.solver.start(this.challenge, (p) => this.updateProgress(p));
      this.updateStatus("Solved! Validating...");
      this.updateProgress({
        attempts: result.attempts,
        elapsedMs: result.durationMs,
        hashesPerSec: Math.floor((result.attempts * 1000) / result.durationMs),
      });

      const validateRes = await this.network.postValidate({
        nonce: this.challenge.nonce,
        solution: result.solution,
        hash: result.hashHex,
      });

      if (validateRes.ok) {
        this.updateStatus("Success!");
        this.uiElements.resultEl.textContent = "OK (200)";
        this.uiElements.resultEl.style.color = "green";
      } else {
        this.updateStatus("Failed");
        this.uiElements.resultEl.textContent = `Rejected: ${validateRes.reason}`;
        this.uiElements.resultEl.style.color = "red";
      }
    } catch (e: any) {
      if (e.message !== "Cancelled") {
        this.updateStatus("Error");
        this.uiElements.resultEl.textContent = `Error: ${e.message}`;
        this.uiElements.resultEl.style.color = "red";
      } else {
        this.updateStatus("Ready");
      }
    } finally {
      this.uiElements.btnStart.disabled = false;
      this.uiElements.btnCancel.disabled = true;
    }
  }

  private cancelSolving() {
    this.solver.cancel();
    this.updateStatus("Ready");
    this.uiElements.btnStart.disabled = false;
    this.uiElements.btnCancel.disabled = true;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  new ChallengeUI();
});
