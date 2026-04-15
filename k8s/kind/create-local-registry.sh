#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source "$SCRIPT_DIR/environment.sh"

if [ -z "$(docker ps -q -f name=$REGISTRY_NAME)" ]; then
  docker run -d --restart=unless-stopped -p "$REGISTRY_PORT:5000" --name "$REGISTRY_NAME" registry:2
fi

docker network connect "kind" "$REGISTRY_NAME" 2>/dev/null || true

