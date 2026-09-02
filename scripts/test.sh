#!/usr/bin/env bash
# Runs unit + BDD tests with the mock LLM profile (default).
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -pl app -am test
