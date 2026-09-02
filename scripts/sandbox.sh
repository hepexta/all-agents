#!/usr/bin/env bash
# Runs the prompt/skill testing sandbox (requires the backend running, see start.sh).
set -euo pipefail
cd "$(dirname "$0")/../sandbox"
pip install -r requirements.txt
python runner.py
pytest tests -v
