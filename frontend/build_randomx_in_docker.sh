#!/usr/bin/env sh
set -eu

DEST_DIR="${1:-./vendor/randomx}"
GIT_REF="${GIT_REF:-master}"
BUN_IMAGE="${BUN_IMAGE:-oven/bun:1.3.12-debian}"

# Force the clone step to re-run when you want it to.
CLONE_CACHEBUST="${CLONE_CACHEBUST:-$(date +%s)}"

docker buildx build \
  --build-arg BUN_IMAGE="$BUN_IMAGE" \
  --build-arg GIT_REF="$GIT_REF" \
  --build-arg CLONE_CACHEBUST="$CLONE_CACHEBUST" \
  --target artifacts \
  --output "type=local,dest=${DEST_DIR}" \
  -f randomx-builder.Dockerfile \
  .
