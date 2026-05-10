ARG NODE_IMAGE=node:22-bookworm-slim

FROM ${NODE_IMAGE} AS build
WORKDIR /workspace
COPY browser/.npmrc ./browser/.npmrc
COPY browser/docker.npmrc ./browser/docker.npmrc
COPY browser/package.json browser/package-lock.json ./browser/
COPY browser/tsconfig.base.json ./browser/tsconfig.base.json
COPY browser/core-contracts/package.json ./browser/core-contracts/package.json
COPY browser/worker/runtime/package.json ./browser/worker/runtime/package.json
COPY browser/widget/base/package.json ./browser/widget/base/package.json
COPY browser/widget/traefik/package.json ./browser/widget/traefik/package.json
COPY browser/worker/sha256/package.json ./browser/worker/sha256/package.json
COPY browser/worker/randomx/package.json ./browser/worker/randomx/package.json
COPY browser/bundle/traefik-sha256/package.json ./browser/bundle/traefik-sha256/package.json
COPY browser/bundle/traefik-bitcoin/package.json ./browser/bundle/traefik-bitcoin/package.json
COPY browser/bundle/traefik-randomx/package.json ./browser/bundle/traefik-randomx/package.json
WORKDIR /workspace/browser
RUN sed -i 's#http://localhost:9001/#http://host.docker.internal:9001/#g' package-lock.json \
    && npm ci

COPY browser/core-contracts ./core-contracts
COPY browser/worker/runtime ./worker/runtime
COPY browser/widget/base ./widget/base
COPY browser/widget/traefik ./widget/traefik
COPY browser/worker/sha256 ./worker/sha256
COPY browser/worker/randomx ./worker/randomx
COPY browser/bundle/traefik-sha256 ./bundle/traefik-sha256
RUN npm run build:libs && npm run build -w @bundle/traefik-sha256

FROM nginx:1.27-alpine AS bundle-image
COPY --from=build /workspace/browser/bundle/traefik-sha256/dist/ /usr/share/nginx/html/
COPY browser/bundle/traefik-sha256/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3001
