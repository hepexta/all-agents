#!/usr/bin/env bash
# Starts the Streamlit chat UI (requires the backend running, see start.sh).
set -euo pipefail
cd "$(dirname "$0")/../ui"
pip install -r requirements.txt
streamlit run app.py
