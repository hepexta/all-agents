#!/usr/bin/env bash
# Integration tests against a real LLM (profile "it", @it-tagged scenarios).
# API credentials are read from ./.env (never committed).
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi
./mvnw -pl app -am test -Pit
