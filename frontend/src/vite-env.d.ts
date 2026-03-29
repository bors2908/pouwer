/// <reference types="vite/client" />

declare module '*?worker' {
  const workerConstructor: {
    new (): Worker;
  };
  export default workerConstructor;
}

declare module '/randomx-web.js' {
  export function randomx_init_cache(key: string): any;
  export function randomx_create_vm(cache: any): any;
}
