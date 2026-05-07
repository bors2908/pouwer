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

const entryPoints = [
  ["src/entries/captcha-traefik-sha256.ts", "captcha-traefik-sha256.js"],
  ["src/entries/captcha-traefik-bitcoin.ts", "captcha-traefik-bitcoin.js"],
  ["src/entries/captcha-traefik-randomx.ts", "captcha-traefik-randomx.js"],
];

const workers = [
  ["src/payloads/sha256/worker.ts", "assets/sha256-worker.js"],
  ["src/payloads/randomx/worker.ts", "assets/randomx-worker.js"],
];

await rm(distDir, {recursive: true, force: true});
await mkdir(resolve(distDir, "assets"), {recursive: true});

await cp(pagesDir, resolve(distDir, "pages"), {recursive: true});
await cp(resolve(pagesDir, "challenge.html"), resolve(distDir, "challenge.html"));
await cp(resolve(pagesDir, "ban.html"), resolve(distDir, "ban.html"));

await Promise.all([
  ...entryPoints.map(([entryPoint, outfile]) =>
    build({
      ...commonOptions,
      entryPoints: [entryPoint],
      outfile: resolve(distDir, outfile),
    })
  ),
  ...workers.map(([entryPoint, outfile]) =>
    build({
      ...commonOptions,
      entryPoints: [entryPoint],
      outfile: resolve(distDir, outfile),
    })
  ),
]);
