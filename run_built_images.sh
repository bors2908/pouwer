#!/usr/bin/env sh
set -eu

BACKEND_IMAGE="${BACKEND_IMAGE:-pouw-backend:local}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-pouw-frontend:local}"

if [ "$#" -eq 0 ]; then
  set -- up -d
fi

BACKEND_IMAGE="${BACKEND_IMAGE}" FRONTEND_IMAGE="${FRONTEND_IMAGE}" \
docker compose \
  -f backend/docker-compose.backend.yml \
  -f frontend/docker-compose.frontend.yml \
  "$@"
