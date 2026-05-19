import {mkdir, rm, readFile, writeFile, readdir} from "node:fs/promises";
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
    outfile: resolve(distDir, "challenge-bitcoin.js"),
  }),
  build({
    ...commonOptions,
    entryPoints: ["@pouwer/worker-sha256/worker"],
    outfile: resolve(distDir, "assets/sha256-worker.js"),
  }),
]);

try {
  const assetPath = resolve(distDir, "assets/sha256-worker.js");
  const mainPath = resolve(distDir, "challenge-bitcoin.js");
  const workerSource = await readFile(assetPath, "utf8");
  const prefix = `globalThis.__POUWER_EMBEDDED_SHA256_WORKER = ${JSON.stringify(workerSource)};\n`;
  const mainContent = await readFile(mainPath, "utf8");
  await writeFile(mainPath, prefix + mainContent, "utf8");
  await rm(assetPath, { force: true });
  try {
    const assetsDir = resolve(distDir, "assets");
    const entries = await readdir(assetsDir);
    if (entries.length === 0) {
      await rm(assetsDir, { force: true });
    }
  } catch (e) {
    // ignore
  }
} catch (e) {
  console.error("Failed to inline worker:", e);
}
