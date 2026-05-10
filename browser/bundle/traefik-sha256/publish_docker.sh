#!/usr/bin/env bash

set -euo pipefail

PROJECT_NAME="pouw-bundle-traefik-sha256"
PROJECT_VERSION="$(node -p "require('./browser/bundle/traefik-sha256/package.json').version")"

docker buildx build \
  --platform linux/amd64 \
  -t "localhost:9002/${PROJECT_NAME}:${PROJECT_VERSION}" \
  -f browser/bundle/traefik-sha256/Dockerfile \
  --target bundle-image \
  --load \
  .
