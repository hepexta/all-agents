# all-agents UI (Python / Streamlit)

Simple chat UI for the master agent of the all-agents platform.

## Run

```bash
pip install -r requirements.txt
streamlit run app.py
```

The UI talks to the Java backend at `http://localhost:8080` (override with
the `ALL_AGENTS_API` environment variable). Start the backend first:

```bash
scripts/start.sh   # or scripts/start.cmd on Windows
```

## Features

- **Presets** — pick one of the master agent presets (`default`, `code-review`, `concise`) in the sidebar.
- **New chat** — resets the current conversation.
- **Load previous chat** — lists persisted conversations (H2-backed) and reloads their history.
- **Chat** — sends messages to `POST /api/chat` and renders the master agent's replies.
