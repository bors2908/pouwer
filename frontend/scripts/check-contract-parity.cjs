const fs = require("node:fs");
const path = require("node:path");

function readFile(relativePath) {
  const absolutePath = path.resolve(__dirname, relativePath);
  return fs.readFileSync(absolutePath, "utf8");
}

function extractQuotedValues(source) {
  return [...source.matchAll(/name\s*=\s*"([^"]+)"/g)].map((match) => match[1]);
}

function extractFrontendJobTypes(source) {
  const match = source.match(/export enum JobType\s*{([\s\S]*?)}/);
  if (!match) {
    throw new Error("Failed to parse frontend JobType enum.");
  }

  return [...match[1].matchAll(/=\s*"([^"]+)"/g)].map((item) => item[1]);
}

function extractBackendJobTypes(source) {
  const match = source.match(/enum class JobType\s*{([\s\S]*?)}/);
  if (!match) {
    throw new Error("Failed to parse backend JobType enum.");
  }

  return match[1]
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean);
}

function assertSetEqual(label, actual, expected) {
  const missing = expected.filter((value) => !actual.includes(value));
  const extra = actual.filter((value) => !expected.includes(value));

  if (missing.length > 0 || extra.length > 0) {
    throw new Error(
      `${label} mismatch.\nMissing: ${missing.join(", ") || "-"}\nExtra: ${extra.join(", ") || "-"}`
    );
  }
}

function main() {
  const frontendTypes = readFile("../src/contracts/types.ts");
  const backendJobType = readFile("../../backend/challenge/src/main/kotlin/ru/itmo/enterprise/pow/model/JobType.kt");
  const backendTaskPayload = readFile("../../backend/challenge/src/main/kotlin/ru/itmo/enterprise/pow/model/TaskPayload.kt");
  const backendResultPayload = readFile("../../backend/challenge/src/main/kotlin/ru/itmo/enterprise/pow/model/ResultPayload.kt");

  const frontendJobTypes = extractFrontendJobTypes(frontendTypes);
  const backendJobTypes = extractBackendJobTypes(backendJobType);
  const backendTaskSubtypeNames = extractQuotedValues(backendTaskPayload);
  const backendResultSubtypeNames = extractQuotedValues(backendResultPayload);

  assertSetEqual("Frontend vs backend JobType", frontendJobTypes, backendJobTypes);
  assertSetEqual("Backend TaskPayload subtype names", backendTaskSubtypeNames, backendJobTypes);
  assertSetEqual("Backend ResultPayload subtype names", backendResultSubtypeNames, backendJobTypes);
}

try {
  main();
  process.stdout.write("Contract parity check passed.\n");
} catch (error) {
  process.stderr.write(`${error.message}\n`);
  process.exit(1);
}
