# all-agents sandbox (Python)

Prompt and skill testing sandbox for the all-agents platform. Runs prompt
files against the backend REST API and evaluates responses — no LLM secrets
are needed here, everything goes through the backend.

## Layout

- `prompts/*.yaml` — prompt/skill catalog. Each file:

  ```yaml
  name: master-greeting
  description: What this prompt exercises
  agent: master            # master (chat) or any agent name (direct execute)
  preset: default          # optional, master only
  message: "Say hello"
  payload: {}              # optional, direct agent calls only (e.g. pdfBase64)
  ```

- `runner.py` — runs prompts and writes results to `results/*.json`.
- `tests/test_prompts.py` — pytest BDD-style (given/when/then) checks against a running backend.

## Usage

```bash
pip install -r requirements.txt

# 1. start the backend (see scripts/start), then:
python runner.py                  # run all prompts
python runner.py master_date      # run a single prompt

# 2. run the test suite (skipped when backend is down):
pytest tests/test_prompts.py -v
```
