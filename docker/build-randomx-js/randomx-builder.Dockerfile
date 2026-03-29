FROM oven/bun:debian AS build

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

# Clone the repo directly instead of copying the local build context.
RUN git clone --depth 1 --branch master https://github.com/bors2908/randomx.js.git /workspace

RUN bun i
RUN bun run scripts/build.ts

# Optional sanity check matching the README.
RUN node examples/randomx.js

FROM scratch AS artifacts
COPY --from=build /workspace/pkg-randomx.js/dist /pkg-randomx.js/dist
COPY --from=build /workspace/pkg-randomwow.js/dist /pkg-randomwow.js/dist
COPY --from=build /workspace/pkg-randomx.js-shared/dist /pkg-randomx.js-shared/dist
COPY --from=build /workspace/pkg-randomwow.js-shared/dist /pkg-randomwow.js-shared/dist
