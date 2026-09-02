"""Simple chat UI for the all-agents master agent (Streamlit).

Talks to the Java backend REST API (default http://localhost:8080,
override with the ALL_AGENTS_API environment variable).

Features:
- master agent presets (loaded from GET /api/presets)
- new chat button (POST /api/conversations)
- load previous chats (GET /api/conversations, GET /api/conversations/{id})
- chat with the master agent (POST /api/chat)
"""

import os

import requests
import streamlit as st

API = os.environ.get("ALL_AGENTS_API", "http://localhost:8080")

st.set_page_config(page_title="All Agents — Master Agent Chat", layout="wide")


def api_get(path: str):
    response = requests.get(f"{API}{path}", timeout=10)
    response.raise_for_status()
    return response.json()


def api_post(path: str, body: dict):
    response = requests.post(f"{API}{path}", json=body, timeout=300)
    response.raise_for_status()
    return response.json()


def load_conversation(conversation_id: str):
    conversation = api_get(f"/api/conversations/{conversation_id}")
    st.session_state.conversation_id = conversation_id
    st.session_state.messages = [
        {"role": entry["role"], "content": entry["content"]}
        for entry in conversation.get("entries", [])
    ]


st.title("Master Agent")

try:
    presets = api_get("/api/presets")
    conversations = api_get("/api/conversations")
    backend_up = True
except requests.RequestException:
    backend_up = False

if not backend_up:
    st.error(f"Backend not reachable at {API}. Start the Java app first (scripts/start).")
    st.stop()

if "conversation_id" not in st.session_state:
    st.session_state.conversation_id = None
if "messages" not in st.session_state:
    st.session_state.messages = []

with st.sidebar:
    st.header("Master agent options")

    preset_names = {p["id"]: p["name"] for p in presets}
    selected_preset = st.selectbox(
        "Preset", list(preset_names), format_func=lambda p: preset_names[p]
    )
    st.session_state.preset = selected_preset

    if st.button("New chat", use_container_width=True):
        st.session_state.conversation_id = None
        st.session_state.messages = []
        st.rerun()

    st.divider()
    st.subheader("Load previous chat")
    titles = {c["id"]: c["title"] for c in conversations}
    if titles:
        selected = st.selectbox(
            "Conversation",
            ["(none)"] + list(titles),
            format_func=lambda c: "(none)" if c == "(none)" else titles[c],
        )
        if selected != "(none)":
            if st.button("Load", use_container_width=True):
                load_conversation(selected)
                st.rerun()
    else:
        st.caption("No previous conversations.")

for message in st.session_state.messages:
    with st.chat_message(message["role"]):
        st.write(message["content"])

if prompt := st.chat_input("Message the master agent..."):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.write(prompt)
    with st.chat_message("assistant"):
        with st.spinner("Master agent is thinking..."):
            reply = api_post("/api/chat", {
                "message": prompt,
                "conversationId": st.session_state.conversation_id,
                "preset": st.session_state.preset,
            })
            st.session_state.conversation_id = reply["conversationId"]
            st.session_state.messages.append({"role": "assistant", "content": reply["content"]})
            st.write(reply["content"])
            if reply.get("blocked"):
                st.caption("(blocked by guardrails)")
