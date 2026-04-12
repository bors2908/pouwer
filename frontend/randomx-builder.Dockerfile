ARG BUN_IMAGE=oven/bun:1.3.12-debian

FROM ${BUN_IMAGE} AS build

ENV DEBIAN_FRONTEND=noninteractive
WORKDIR /workspace

#TODO: Rewrite to buildx to cache and reuse builder as image
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

ARG CLONE_CACHEBUST=1
ARG GIT_REF=master

# Clone the repo directly instead of copying the local build context.
RUN set -eux; \
    echo "clone cache bust: ${CLONE_CACHEBUST}"; \
    git clone --depth 1 --branch "${GIT_REF}" https://github.com/bors2908/randomx.js.git /workspace

RUN bun install --frozen-lockfile
RUN bun run scripts/build.ts

RUN set -eux; \
    outDir="/workspace/pkg-randomx.js-shared/dist/web"; \
    inputJs="${outDir}/index.js"; \
    hash="$(sha256sum "${inputJs}" | awk '{print $1}')"; \
    hashedJs="${outDir}/randomx-shared-web.${hash}.js"; \
    mv "${inputJs}" "${hashedJs}"; \
    if [ -f "${outDir}/index.js.map" ]; then \
      mv "${outDir}/index.js.map" "${outDir}/randomx-shared-web.${hash}.js.map"; \
    fi; \
    printf '%s  %s\n' "${hash}" "randomx-shared-web.${hash}.js" > "${outDir}/randomx-shared-web.${hash}.sha256"; \
    mkdir -p /artifact; \
    cp "${hashedJs}" /artifact/; \
    [ -f "${outDir}/randomx-shared-web.${hash}.js.map" ] && cp "${outDir}/randomx-shared-web.${hash}.js.map" /artifact/ || true; \
    cp "${outDir}/randomx-shared-web.${hash}.sha256" /artifact/

FROM scratch AS artifacts
COPY --from=build /artifact/ /
