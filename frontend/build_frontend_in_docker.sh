#!/usr/bin/env sh
set -eu

FRONTEND_IMAGE="${FRONTEND_IMAGE:-pouw-frontend:local}"
COMPOSE_DEST_DIR="${1:-./out/frontend-compose}"

docker buildx build \
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
