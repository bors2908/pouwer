/// <reference types="vite/client" />

declare module '*?worker' {
  const workerConstructor: {
    new (): Worker;
  };
  export default workerConstructor;
}

declare module '/randomx-web.js' {
  export function mine(job: Job, config: Config = {}): any;

  export interface Job {
      blob: string
      job_id: string
      target: string
      height: number
      seed_hash: string
  }

    export type Config = {
        target_threads?: number
        callbacks?: Partial<MinerCallbacks>
    }

    export type WorkerEventWorkerReady = {
        type: 'event_worker_ready'
        miner_id: string
    }

    export type WorkerEventJobStarted = {
        type: 'event_job_started'
        miner_id: string
        job_id: string
        nonce_start: number
        nonce_end: number
        target: bigint
    }

    export type WorkerEventJobDisposed = {
        type: 'event_job_disposed'
        miner_id: string
    }

    export type WorkerEventNonceSpaceExhausted = {
        type: 'event_nonce_space_exhausted'
        miner_id: string
        job_id: string
    }

    export type WorkerEventResultFound = {
        type: 'event_result_found'
        miner_id: string
        job_id: string
        hash_count: number
        nonce: number
        result: Uint8Array
    }

    export type WorkerPong = {
        type: 'pong'

        miner_id: string

        stats: {
            hashes_per_second: number
            hashes_total: number
        }
    }

    export type MinerCallbacks = {
        on_cache_initialising: () => void
        on_cache_initialised: (duration_ms: number) => void
        on_worker_ready: (event: WorkerEventWorkerReady) => void
        on_job_started: (event: WorkerEventJobStarted) => void
        on_job_disposed: (event: WorkerEventJobDisposed) => void
        on_nonce_space_exhausted: (event: WorkerEventNonceSpaceExhausted) => void
        on_result_found: (event: WorkerEventResultFound) => void
        on_pong: (event: WorkerPong) => void
    }
}
