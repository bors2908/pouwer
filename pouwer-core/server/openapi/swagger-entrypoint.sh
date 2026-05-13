#!/bin/sh
set -eu

OPENAPI_FILE="/docs/openapi.yml"

if [ -z "${GATEWAY_HOST:-}" ]; then
  echo "[startup] GATEWAY_HOST not set — leaving $OPENAPI_FILE untouched"
else
  echo "[startup] Replacing placeholder in $OPENAPI_FILE with: $GATEWAY_HOST"

  ESCAPED=$(printf '%s' "$GATEWAY_HOST" | sed 's/[\/&]/\\&/g')

  if grep -q '\${GATEWAY_HOST}' "$OPENAPI_FILE"; then
    sed -i "s|\${GATEWAY_HOST}|$ESCAPED|g" "$OPENAPI_FILE"
  else
    echo "[startup] Placeholder not found in $OPENAPI_FILE — skipping replacement"
  fi
fi

exec /docker-entrypoint.sh nginx -g "daemon off;"
