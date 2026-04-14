/// <reference types="vite/client" />

declare module '*?worker' {
  const workerConstructor: {
    new (): Worker;
  };
  export default workerConstructor;
}

declare module '/randomx-web.js' {
  export function mine(job: Job): any;

  export interface Job {
      blob: string
      job_id: string
      target: string
      height: number
      seed_hash: string
  }
}
