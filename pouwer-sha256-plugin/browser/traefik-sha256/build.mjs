import {cp, mkdir, rm} from "node:fs/promises";
import {resolve} from "node:path";
import {build} from "esbuild";

const rootDir = import.meta.dirname;
const distDir = resolve(rootDir, "dist");

const commonOptions = {
  absWorkingDir: rootDir,
  bundle: true,
  format: "esm",
  minify: true,
  sourcemap: false,
  target: ["es2022"],
  logLevel: "info",
};

await rm(distDir, {recursive: true, force: true});
await mkdir(resolve(distDir, "assets"), {recursive: true});

await Promise.all([
  build({
    ...commonOptions,
    entryPoints: [resolve(rootDir, "src/index.ts")],
    outfile: resolve(distDir, "captcha-traefik-sha256.js"),
  }),
  build({
    ...commonOptions,
    entryPoints: ["@pouwer/worker-sha256/worker"],
    outfile: resolve(distDir, "assets/sha256-worker.js"),
  }),
]);
