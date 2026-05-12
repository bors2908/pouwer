#!/usr/bin/env bash

set -euo pipefail

PROJECT_NAME="pouwer-bundle-traefik-bitcoin"
PROJECT_VERSION="$(node -p "require('./browser/bundle/traefik-bitcoin/package.json').version")"

docker buildx build \
  --platform linux/amd64 \
  -t "localhost:9002/${PROJECT_NAME}:${PROJECT_VERSION}" \
  -f browser/bundle/traefik-bitcoin/Dockerfile \
  --target bundle-image \
  --load \
  .
