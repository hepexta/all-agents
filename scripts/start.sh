#!/usr/bin/env bash
# Builds and starts the backend (REST + A2A endpoints on :8080).
# API credentials are read from ./.env (never committed).
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi
./mvnw -pl app -am package -DskipTests
java -jar app/target/all-agents-app-0.0.1-SNAPSHOT.jar
