#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:${APP_PORT:-8088}}"
echo "Running API smoke test against ${BASE_URL}"
python3 "$(dirname "$0")/api-smoke.py" --base-url "${BASE_URL}"
