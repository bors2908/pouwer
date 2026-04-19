ARG BUN_IMAGE=oven/bun:1.3.12-debian

FROM ${BUN_IMAGE} AS randomx-builder

ENV DEBIAN_FRONTEND=noninteractive
WORKDIR /workspace

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    clang \
    lld \
    binaryen \
    wabt \
    git \
    ca-certificates \
    nodejs \
  && rm -rf /var/lib/apt/lists/*

ARG GIT_REF=master

# Clone the repo directly instead of copying the local build context.
RUN set -eux; \
    git clone --depth 1 --branch "${GIT_REF}" --tags https://github.com/bors2908/randomx.js.git /workspace

RUN bun install --frozen-lockfile
RUN bun run scripts/build.ts

RUN set -eux; \
    outDir="/workspace/pkg-randomx.js-shared/dist/web"; \
    artifactBase="randomx-web"; \
    inputJs="${outDir}/index.js"; \
    shortCommit="$(git -C /workspace rev-parse --short=12 HEAD)"; \
    taggedJs="${outDir}/${artifactBase}.${shortCommit}.js"; \
    mv "${inputJs}" "${taggedJs}"; \
    if [ -f "${outDir}/index.js.map" ]; then \
      mv "${outDir}/index.js.map" "${outDir}/${artifactBase}.${shortCommit}.js.map"; \
    fi; \
    mkdir -p /artifact; \
    cp "${taggedJs}" /artifact/; \
    [ -f "${outDir}/${artifactBase}.${shortCommit}.js.map" ] && cp "${outDir}/${artifactBase}.${shortCommit}.js.map" /artifact/ || true

FROM scratch AS randomx-builder-image
COPY --from=randomx-builder /artifact/ /artifact/

FROM scratch AS artifacts
COPY --from=randomx-builder /artifact/ /
