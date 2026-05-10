import {cp, mkdir, rm} from "node:fs/promises";
import {resolve} from "node:path";
import {build} from "esbuild";

const rootDir = import.meta.dirname;
const distDir = resolve(rootDir, "dist");
const pagesDir = resolve(rootDir, "pages");

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

await cp(pagesDir, resolve(distDir, "pages"), {recursive: true});
await cp(resolve(pagesDir, "challenge.html"), resolve(distDir, "challenge.html"));
await cp(resolve(pagesDir, "ban.html"), resolve(distDir, "ban.html"));

await Promise.all([
  build({
    ...commonOptions,
    entryPoints: [resolve(rootDir, "src/index.ts")],
    outfile: resolve(distDir, "captcha-traefik-bitcoin.js"),
  }),
  build({
    ...commonOptions,
    entryPoints: ["@worker/sha256/worker"],
    outfile: resolve(distDir, "assets/sha256-worker.js"),
  }),
]);
