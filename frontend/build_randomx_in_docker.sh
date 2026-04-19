#!/usr/bin/env sh
set -eu

DEST_DIR="./vendor/randomx"
GIT_REF="${GIT_REF:-master}"
BUN_IMAGE="${BUN_IMAGE:-oven/bun:1.3.12-debian}"
BUILDER_IMAGE="${BUILDER_IMAGE:-pouw-randomx-builder:local}"
LOAD_BUILDER_IMAGE=0
OUTPUT_ARTIFACTS=1

# Force the clone step to re-run when you want it to.
CLONE_CACHEBUST="${CLONE_CACHEBUST:-$(date +%s)}"

for arg in "$@"; do
  case "$arg" in
    --dest=*)
      DEST_DIR="${arg#*=}"
      ;;
    --builder-image=*)
      BUILDER_IMAGE="${arg#*=}"
      ;;
    --load-builder-image)
      LOAD_BUILDER_IMAGE=1
      ;;
    --no-output-artifacts)
      OUTPUT_ARTIFACTS=0
      ;;
    --*)
      echo "Unknown option: $arg" >&2
      exit 1
      ;;
    *)
      DEST_DIR="$arg"
      ;;
  esac
done

if [ "$LOAD_BUILDER_IMAGE" -eq 1 ]; then
  docker buildx build \
    --build-arg BUN_IMAGE="$BUN_IMAGE" \
    --build-arg GIT_REF="$GIT_REF" \
    --build-arg CLONE_CACHEBUST="$CLONE_CACHEBUST" \
    --target randomx-builder-image \
    --tag "${BUILDER_IMAGE}" \
    --load \
    -f randomx-builder.Dockerfile \
    .
fi

if [ "$OUTPUT_ARTIFACTS" -eq 1 ]; then
  docker buildx build \
    --build-arg BUN_IMAGE="$BUN_IMAGE" \
    --build-arg GIT_REF="$GIT_REF" \
    --build-arg CLONE_CACHEBUST="$CLONE_CACHEBUST" \
    --target artifacts \
    --output "type=local,dest=${DEST_DIR}" \
    -f randomx-builder.Dockerfile \
    .
fi
