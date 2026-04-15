#!/usr/bin/env bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
export CLUSTER_NAME="crowdsec-poc"
export KIND_CONFIG_FILE="$SCRIPT_DIR/kind-config.yaml"
export REGISTRY_NAME="kind-registry"
export REGISTRY_PORT="5000"

