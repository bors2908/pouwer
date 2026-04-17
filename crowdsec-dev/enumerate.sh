#!/usr/bin/env bash

find . -type f \( -name "*" \) | while read -r f; do
  echo "==> $f"
  cat "$f"
  echo
done
