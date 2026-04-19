#!/usr/bin/env sh
set -eu

RANDOMX_BUILDER_IMAGE="${RANDOMX_BUILDER_IMAGE:-pouw-randomx-builder:local}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-pouw-frontend:local}"
COMPOSE_DEST_DIR="${1:-./out/frontend-compose}"

GIT_REF="${GIT_REF:-master}"
BUN_IMAGE="${BUN_IMAGE:-oven/bun:1.3.12-debian}"
CLONE_CACHEBUST="${CLONE_CACHEBUST:-$(date +%s)}"
NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmjs.org/}"

docker buildx build \
  --build-arg BUN_IMAGE="$BUN_IMAGE" \
  --build-arg GIT_REF="$GIT_REF" \
  --build-arg CLONE_CACHEBUST="$CLONE_CACHEBUST" \
  --target randomx-builder-image \
  --tag "${RANDOMX_BUILDER_IMAGE}" \
  --load \
  -f randomx-builder.Dockerfile \
  .

docker buildx build \
  --build-arg RANDOMX_BUILDER_IMAGE="${RANDOMX_BUILDER_IMAGE}" \
  --build-arg NPM_REGISTRY="${NPM_REGISTRY}" \
  --target frontend-image \
  --tag "${FRONTEND_IMAGE}" \
  --load \
  -f frontend-complete.Dockerfile \
  .

docker buildx build \
  --target compose \
  --output "type=local,dest=${COMPOSE_DEST_DIR}" \
  -f frontend-complete.Dockerfile \
  .
