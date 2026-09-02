# all-agents

Agentic platform (Java 25, Spring Boot 4.1.1, Spring AI 2.0.1) with a **master agent**
orchestrating a growing team of specialist agents (~100 planned). The first specialist
agent extracts data from PDF documents.

## Modules

| Module    | Tech          | Purpose                                                        |
|-----------|---------------|----------------------------------------------------------------|
| `core`    | Java          | Hexagonal core: domain, ports, agent framework, guardrails API |
| `app`     | Java / Spring | Agents (master + pdf-extractor), services, REST + A2A adapters, H2 chat memory |
| `ui`      | Python        | Streamlit chat UI with master-agent presets and chat history   |
| `sandbox` | Python        | Prompt/skill testing harness + pytest BDD-style checks         |

## Architecture (hexagonal)

```
        adapters.in (REST /api/*, A2A JSON-RPC /a2a/*)
                          │
   application services ──┤   ChatService · AgentRegistryService · AgentLifecycleService
   (use cases)            │   AgentRuntimeService · ConversationService · PresetService
                          │
        domain (core) ────┤   Agent/AbstractAgent · ports (AgentRegistry, AgentRuntime,
                          │   A2aClient, AgentEventBus, ToolCatalog, ChatMemory...) · DTOs
                          │
       adapters.out ──────┘   H2 (chat memory + conversations) · InProcess/Http A2A clients
                              InMemoryToolCatalog · InMemoryAgentEventBus (Kafka seam)
```

- **Master agent** (`master`): routes requests, delegates to specialists over A2A
  (`delegateToAgent` tool), controls agent lifecycle (`startAgent`/`stopAgent`/
  `getAgentStatus`/`listAgents`), knows every agent, skill and tool (`toolSearchTool` —
  Spring AI ToolSearch, `getCurrentDate`).
- **PDF agent** (`pdf-extractor`): PDFBox text extraction + LLM formatting; accepts
  `pdfBase64` or `pdfPath` payloads.
- **Guardrails** (input + output): prompt-injection deny-list, sensitive-data redaction
  (emails, API keys, cards, phones), length limits. Applied via a ChatClient advisor
  (chat path) and `AgentRuntimeService` (direct agent calls).
- **Chat memory**: Spring AI `ChatMemory` backed by embedded H2 (file-based `./data/`,
  survives restarts). Conversations (metadata + entries) are also persisted in H2.
- **A2A protocol**: every agent exposes an agent card (`GET /a2a/agents/{name}`) and
  JSON-RPC 2.0 `message/send`, `message/stream` (sync fallback), `tasks/get`.
- **Kafka-ready async**: all agent events (start/stop/request completed/failed) flow
  through the `AgentEventBus` port. The in-memory implementation is the default; a Kafka
  adapter can replace it without touching the domain (see roadmap below).

## Quickstart

```bash
scripts/build.sh     # full build: tests (mock LLM) + 100% line coverage gate
scripts/start.sh     # build + run backend on :8080 (reads ./.env for API credentials)
scripts/ui.sh        # Streamlit UI (http://localhost:8501)
scripts/sandbox.sh   # prompt testing against the running backend
```

Windows: use the `.cmd` variants. API credentials live in `.env` (gitignored) — no
secrets in yaml properties.

## REST API

- `POST /api/chat` `{message, conversationId?, preset?}` → `{conversationId, content, blocked}`
- `POST /api/conversations` / `GET /api/conversations` / `GET /api/conversations/{id}`
- `GET /api/presets`
- `GET /api/agents` · `POST /api/agents/{name}/start|stop` · `GET /api/agents/{name}/status` · `POST /api/agents/{name}/execute`
- `GET /api/tools`
- A2A: `GET /a2a/agents/{name}` · `POST /a2a/agents/{name}` (JSON-RPC `message/send`, `message/stream`, `tasks/get`)

## Tests

- **BDD** (Cucumber, Given/When/Then): `features/mock/*.feature` — master agent and
  pdf agent scenarios against a **scripted mock LLM** (`MockChatModel`, no network).
- **Mock by default**: `scripts/test.sh` runs with the `mock` profile.
- **Integration profile**: `scripts/it-test.sh` runs `@it` scenarios against the real
  LLM (credentials from `.env`; switches via the `it` Maven profile).
- **Coverage**: JaCoCo enforces **100% line coverage** on `com.hepexta.allagents.*`
  (excluding `AllAgentsApplication` and the `config` package). `scripts/build.sh` fails
  otherwise.

## Adding the next agent

See [docs/ADDING-A-NEW-AGENT.md](docs/ADDING-A-NEW-AGENT.md) — one class + one
definition + BDD feature; registration, A2A card, lifecycle and tool search come for free.

## Kafka async roadmap (future)

The `AgentEventBus` port is the seam. Steps to switch to async Kafka mode:

1. Add `spring-kafka` dependency.
2. Implement `KafkaAgentEventBus`: `publish()` → producer to topic `agent-events`;
   a consumer replays events for `subscribe()` listeners.
3. Register it conditionally (`@ConditionalOnProperty(name = "app.messaging.provider", havingValue = "kafka")`).
4. Move `AgentRuntimeService.execute` to a request/reply flow (`agent-requests` /
   `agent-responses` topics) using the existing A2A `Task` + `TaskStore` for `tasks/get`.
