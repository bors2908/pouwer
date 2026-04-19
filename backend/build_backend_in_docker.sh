#!/usr/bin/env sh
set -eu

IMAGE_NAME="${IMAGE_NAME:-pouw-backend:local}"
COMPOSE_DEST_DIR="${1:-./out/backend-compose}"
KT_STRATUM_REPO="${KT_STRATUM_REPO:-https://github.com/bors2908/KtStratum}"
KT_STRATUM_REF="${KT_STRATUM_REF:-p2pool-specific}"

docker buildx build \
  --build-arg KT_STRATUM_REPO="${KT_STRATUM_REPO}" \
  --build-arg KT_STRATUM_REF="${KT_STRATUM_REF}" \
  --target backend-image \
  --tag "${IMAGE_NAME}" \
  --load \
  -f backend-buildx.Dockerfile \
  .

docker buildx build \
  --target compose \
  --output "type=local,dest=${COMPOSE_DEST_DIR}" \
  -f backend-buildx.Dockerfile \
  .
