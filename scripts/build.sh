#!/usr/bin/env bash
# Full build: compiles, runs all tests (mock LLM profile) and enforces 100% line coverage.
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw clean verify
