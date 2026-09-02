"""Prompt/skill testing harness for the all-agents platform.

Runs every prompt in ./prompts/*.yaml against the backend REST API and
writes the responses to ./results/<name>.json.

Usage:
    python runner.py                # run all prompts
    python runner.py master_date    # run a single prompt by name
"""

import json
import sys
from pathlib import Path

import requests
import yaml

API = "http://localhost:8080"
PROMPTS_DIR = Path(__file__).parent / "prompts"
RESULTS_DIR = Path(__file__).parent / "results"


def run_prompt(prompt_file: Path) -> dict:
    spec = yaml.safe_load(prompt_file.read_text(encoding="utf-8"))
    if spec.get("agent") == "master":
        response = requests.post(
            f"{API}/api/chat",
            json={
                "message": spec["message"],
                "preset": spec.get("preset"),
            },
            timeout=300,
        )
        response.raise_for_status()
        result = {
            "prompt": spec["name"],
            "request": spec["message"],
            "reply": response.json()["content"],
            "conversationId": response.json()["conversationId"],
            "blocked": response.json()["blocked"],
        }
    else:
        response = requests.post(
            f"{API}/api/agents/{spec['agent']}/execute",
            json={"instruction": spec["message"], "payload": spec.get("payload", {})},
            timeout=300,
        )
        response.raise_for_status()
        result = {"prompt": spec["name"], "request": spec["message"], "result": response.json()}
    return result


def main() -> None:
    RESULTS_DIR.mkdir(exist_ok=True)
    only = sys.argv[1] if len(sys.argv) > 1 else None
    files = sorted(PROMPTS_DIR.glob("*.yaml"))
    if only:
        files = [f for f in files if f.stem == only]
    if not files:
        print(f"No prompts found{f' matching {only}' if only else ''} in {PROMPTS_DIR}")
        sys.exit(1)
    for prompt_file in files:
        try:
            result = run_prompt(prompt_file)
        except requests.RequestException as exc:
            result = {"prompt": prompt_file.stem, "error": str(exc)}
        out = RESULTS_DIR / f"{prompt_file.stem}.json"
        out.write_text(json.dumps(result, indent=2), encoding="utf-8")
        print(f"[{prompt_file.stem}] -> {out}")


if __name__ == "__main__":
    main()
