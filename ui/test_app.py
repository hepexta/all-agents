"""Tests for the Streamlit chat UI (ui/app.py).

HTTP is fully mocked via monkeypatching requests.get / requests.post, so the
tests never need a running backend. The app is driven with the official
Streamlit AppTest framework (streamlit.testing.v1).
"""

from pathlib import Path

import requests
from streamlit.testing.v1 import AppTest

APP = str(Path(__file__).parent / "app.py")

PRESETS = [
    {"id": "default", "name": "Default"},
    {"id": "concise", "name": "Concise"},
]

CONVERSATIONS = [
    {
        "id": "c1",
        "title": "First chat",
        "preset": "default",
        "createdAt": "2026-08-31T10:00:00",
        "entries": [],
    }
]

CONVERSATION_DETAIL = {
    "id": "c1",
    "title": "First chat",
    "preset": "default",
    "createdAt": "2026-08-31T10:00:00",
    "entries": [
        {"role": "user", "content": "hello", "timestamp": "2026-08-31T10:00:01"},
        {"role": "assistant", "content": "hi there", "timestamp": "2026-08-31T10:00:02"},
    ],
}


class FakeResponse:
    def __init__(self, json_data=None, status_error=None):
        self._json = json_data
        self._status_error = status_error

    def raise_for_status(self):
        if self._status_error is not None:
            raise self._status_error

    def json(self):
        return self._json


class FakeApi:
    """Replaces requests.get / requests.post and records every call."""

    def __init__(self, monkeypatch, presets=None, conversations=None,
                 conversation=None, chat_reply=None):
        self.presets = presets if presets is not None else PRESETS
        self.conversations = conversations if conversations is not None else CONVERSATIONS
        self.conversation = conversation
        self.chat_reply = chat_reply
        self.get_error = None
        self.post_error = None
        self.calls = []
        monkeypatch.setattr(requests, "get", self.get)
        monkeypatch.setattr(requests, "post", self.post)

    def get(self, url, timeout=None):
        self.calls.append(("GET", url))
        if self.get_error is not None:
            raise self.get_error
        if url.endswith("/api/presets"):
            return FakeResponse(self.presets)
        if url.endswith("/api/conversations"):
            return FakeResponse(self.conversations)
        if url.startswith("http://localhost:8080/api/conversations/"):
            return FakeResponse(self.conversation)
        raise AssertionError(f"Unexpected GET {url}")

    def post(self, url, json=None, timeout=None):
        self.calls.append(("POST", url, json))
        if url.endswith("/api/chat"):
            return FakeResponse(self.chat_reply, status_error=self.post_error)
        raise AssertionError(f"Unexpected POST {url}")


def run_app(monkeypatch, **api_kwargs):
    api = FakeApi(monkeypatch, **api_kwargs)
    app = AppTest.from_file(APP)
    app.run()
    return app, api


def chat_body(api):
    return [call[2] for call in api.calls if call[0] == "POST"][-1]


def test_backend_up_renders_ui(monkeypatch):
    app, api = run_app(monkeypatch)

    assert not app.exception
    assert app.title[0].value == "Master Agent"
    assert app.selectbox[0].value == "default"
    assert ("GET", "http://localhost:8080/api/presets") in api.calls
    assert ("GET", "http://localhost:8080/api/conversations") in api.calls
    assert app.session_state["conversation_id"] is None
    assert app.session_state["messages"] == []


def test_backend_down_shows_error(monkeypatch):
    api = FakeApi(monkeypatch)
    api.get_error = requests.ConnectionError("connection refused")

    app = AppTest.from_file(APP)
    app.run()

    assert any("Backend not reachable" in e.value for e in app.error)


def test_backend_http_500_shows_error(monkeypatch):
    api = FakeApi(monkeypatch)
    api.get_error = requests.HTTPError("500 Server Error")

    app = AppTest.from_file(APP)
    app.run()

    assert any("Backend not reachable" in e.value for e in app.error)


def test_chat_sends_message_and_renders_reply(monkeypatch):
    app, api = run_app(monkeypatch, chat_reply={
        "conversationId": "c1",
        "content": "Hello!",
        "blocked": False,
    })

    app.chat_input[0].set_value("hi").run()

    assert not app.exception
    assert chat_body(api) == {
        "message": "hi",
        "conversationId": None,
        "preset": "default",
    }
    assert app.session_state["conversation_id"] == "c1"
    assert app.session_state["messages"] == [
        {"role": "user", "content": "hi"},
        {"role": "assistant", "content": "Hello!"},
    ]
    assert app.markdown[-1].value == "Hello!"


def test_chat_reuses_conversation_id(monkeypatch):
    app, api = run_app(monkeypatch, chat_reply={
        "conversationId": "c1",
        "content": "Hello!",
        "blocked": False,
    })
    app.chat_input[0].set_value("hi").run()

    app.chat_input[0].set_value("again").run()

    assert chat_body(api) == {
        "message": "again",
        "conversationId": "c1",
        "preset": "default",
    }
    assert app.session_state["conversation_id"] == "c1"
    assert len(app.session_state["messages"]) == 4


def test_blocked_reply_shows_caption(monkeypatch):
    app, _ = run_app(monkeypatch, chat_reply={
        "conversationId": "c1",
        "content": "Request blocked: prompt injection",
        "blocked": True,
    })

    app.chat_input[0].set_value("hi").run()

    assert not app.exception
    assert any("(blocked by guardrails)" in c.value for c in app.caption)
    assert app.session_state["messages"][-1] == {
        "role": "assistant",
        "content": "Request blocked: prompt injection",
    }


def test_new_chat_resets_session(monkeypatch):
    app, _ = run_app(monkeypatch, chat_reply={
        "conversationId": "c1",
        "content": "Hello!",
        "blocked": False,
    })
    app.chat_input[0].set_value("hi").run()

    app.button[0].click().run()

    assert not app.exception
    assert app.session_state["conversation_id"] is None
    assert app.session_state["messages"] == []


def test_load_previous_conversation(monkeypatch):
    app, api = run_app(monkeypatch, conversation=CONVERSATION_DETAIL)

    app.selectbox[1].select("c1").run()
    app.button[1].click().run()

    assert not app.exception
    assert ("GET", "http://localhost:8080/api/conversations/c1") in api.calls
    assert app.session_state["conversation_id"] == "c1"
    assert app.session_state["messages"] == [
        {"role": "user", "content": "hello"},
        {"role": "assistant", "content": "hi there"},
    ]
    assert app.markdown[0].value == "hello"
    assert app.markdown[1].value == "hi there"


def test_no_previous_conversations_shows_caption(monkeypatch):
    app, _ = run_app(monkeypatch, conversations=[])

    assert not app.exception
    assert any("No previous conversations." == c.value for c in app.caption)
    assert len(app.selectbox) == 1


def test_chat_backend_500_surfaces_as_exception(monkeypatch):
    api = FakeApi(monkeypatch, chat_reply={
        "conversationId": "c1",
        "content": "Hello!",
        "blocked": False,
    })
    api.post_error = requests.HTTPError("500 Server Error")
    app = AppTest.from_file(APP)
    app.run()

    app.chat_input[0].set_value("hi").run()

    assert any("500 Server Error" in str(e.value) for e in app.exception)
