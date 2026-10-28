#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "This will stop the stack and DELETE the MySQL volume (all demo data)."
read -r -p "Continue? [y/N] " answer
if [[ ! "${answer}" =~ ^[Yy]$ ]]; then
  echo "Aborted."
  exit 0
fi

docker compose down -v
docker compose up -d --build
docker compose ps
