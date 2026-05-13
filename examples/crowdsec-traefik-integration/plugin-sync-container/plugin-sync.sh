#!/bin/sh
set -eu

repo_url="${PLUGIN_REPO_URL:-http://host.docker.internal:9001/repository/maven-hosted}"
target_dir="${PLUGIN_TARGET_DIR:-/plugins}"
clean="${PLUGIN_SYNC_CLEAN:-true}"

if [ "$#" -eq 0 ]; then
  echo "usage: plugin-sync.sh [--repo-url URL] [--target-dir DIR] [--no-clean] --artifact group:artifact:version ..." >&2
  exit 1
fi

artifacts=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    --repo-url)
      repo_url="$2"
      shift 2
      ;;
    --target-dir)
      target_dir="$2"
      shift 2
      ;;
    --no-clean)
      clean="false"
      shift
      ;;
    --artifact)
      artifacts="${artifacts}${2}
"
      shift 2
      ;;
    *)
      echo "unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

if [ -z "$artifacts" ]; then
  echo "no artifacts requested" >&2
  exit 1
fi

mkdir -p "$target_dir"

if [ "$clean" = "true" ]; then
  find "$target_dir" -mindepth 1 -maxdepth 1 -type f -name '*.jar' -delete
fi

download_artifact() {
  coord="$1"
  group_id=$(printf '%s' "$coord" | cut -d: -f1)
  artifact_id=$(printf '%s' "$coord" | cut -d: -f2)
  version=$(printf '%s' "$coord" | cut -d: -f3)

  if [ -z "$group_id" ] || [ -z "$artifact_id" ] || [ -z "$version" ]; then
    echo "invalid artifact coordinate: $coord" >&2
    exit 1
  fi

  group_path=$(printf '%s' "$group_id" | tr '.' '/')
  jar_name="${artifact_id}-${version}.jar"
  artifact_url="${repo_url}/${group_path}/${artifact_id}/${version}/${jar_name}"
  output_file="${target_dir}/${jar_name}"

  echo "fetching ${coord} -> ${output_file}"
  curl -fsSL "$artifact_url" -o "$output_file"
}

printf '%s' "$artifacts" | while IFS= read -r coord; do
  [ -n "$coord" ] || continue
  download_artifact "$coord"
done
