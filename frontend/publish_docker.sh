#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "${SCRIPT_DIR}"

REGISTRY_HOST="${REGISTRY_HOST:-localhost:9002}"
PROJECT_NAME="pouw-frontend"
PROJECT_VERSION="$(node -p "require('./package.json').version")"
FRONTEND_IMAGE="${REGISTRY_HOST}/${PROJECT_NAME}:${PROJECT_VERSION}"

docker buildx build \
  --target frontend-image \
  --tag "${FRONTEND_IMAGE}" \
  --push \
  -f Dockerfile \
  .
