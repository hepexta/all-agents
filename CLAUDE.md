# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Agentic platform (Java 25, Spring Boot 4.1.1, Spring AI 2.0.1) with a **master agent**
orchestrating a growing team of specialist agents (~100 planned; first specialist:
PDF data extraction). Hexagonal architecture, A2A protocol, REST API, H2-backed chat
memory, guardrails, Kafka-ready event bus. Spec: `Executive-agent.md`.

Modules (Maven multi-module): `core` (hexagonal core: domain, ports, agent framework),
`app` (Spring Boot: agents, services, adapters), `ui` (Python/Streamlit chat),
`sandbox` (Python prompt testing). `ui` and `sandbox` are pom-packaging containers —
Python is not built by Maven.

## Commands

Use the Maven wrapper (Maven 3.9.16). In bash: `./mvnw`; in cmd/PowerShell: `mvnw.cmd`.
Scripts in `scripts/` (`.sh` + `.cmd` variants):

- `scripts/build.sh` — full build: tests + JaCoCo 100% line-coverage gate (`./mvnw clean verify`)
- `scripts/test.sh` — unit + BDD tests, mock LLM profile (default): `./mvnw -pl app -am test`
- `scripts/it-test.sh` — `@it` scenarios against the real LLM (sources `.env`, `-Pit` profile)
- `scripts/start.sh` — build + run backend on :8080 (sources `.env`)
- `scripts/ui.sh` / `scripts/sandbox.sh` — Streamlit UI / Python prompt tests

Single test: `./mvnw -pl app -am test -Dtest=ClassName#methodName`.

## Architecture

**Hexagonal**: `adapters.in.rest` (REST `/api/*`, A2A JSON-RPC `/a2a/agents/{name}`) →
`application` services (ChatService, AgentRegistryService, AgentLifecycleService,
AgentRuntimeService, ConversationService, PresetService, TaskStore) → `core` domain/ports
(Agent, AgentRegistry, AgentRuntime, A2aClient, AgentEventBus, ToolCatalog, ChatMemory,
Clock) → `adapters.out` (H2 chat memory + conversations, InProcess/Http A2A clients,
InMemoryToolCatalog, InMemoryAgentEventBus).

Key design points:

- **Agents** extend `core`'s `AbstractAgent` and are `@Component`s; the registry
  auto-discovers, registers and starts every `Agent` bean. Adding an agent = one class
  (see `docs/ADDING-A-NEW-AGENT.md`).
- **Master agent** orchestrates via `MasterTools` (@Tool methods: delegateToAgent over
  A2A, listAgents, startAgent/stopAgent/getAgentStatus, getCurrentDate) plus the
  official Spring AI `ToolSearchTool` (indexed `RegexToolIndex`, session `"default"`,
  advisor-context key `TOOL_SEARCH_TOOL_SESSION_ID_KEY` — set per call in `MasterAgent`).
- **Guardrails**: `PromptInjectionGuardrail` (deny phrases), `SensitiveDataGuardrail`
  (output redaction), `LengthGuardrail`; composed in `CompositeGuardrail`. Applied via
  `GuardrailAdvisor` (chat path, throws `GuardrailBlockedException` before the model
  call) and `AgentRuntimeService` (direct agent path, input + output).
- **Spring AI 2.0.1 specifics** (differs from 1.x — do not use 1.x APIs):
  - Advisors implement `BaseAdvisor` (`before(ChatClientRequest, AdvisorChain)` /
    `after(...)`); `RequestResponseAdvisor`/`AdvisedRequest` no longer exist.
  - `ChatMemory`: `get(String)` (no lastN), `add(String, List<Message>)`; conversation
    id passed per call via advisor params: `.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))`.
  - `Message.getText()` (not `getContent()`); `AssistantMessage.ToolCall` record +
    `AssistantMessage.builder()`; finish reason is a String in
    `ChatGenerationMetadata.builder().finishReason("TOOL_CALLS"|"STOP")`.
  - **Tool-calling loop only runs when the effective prompt options are
    `ToolCallingChatOptions`** — they are derived from
    `chatModel.getOptions().mutate().combineWith(customizer)`, so set
    `.options(ToolCallingChatOptions.builder())` per call AND return
    `ToolCallingChatOptions` from any mock ChatModel's `getOptions()`.
  - Tool execution context (e.g. ToolSearch session) comes from
    `builder.defaultToolContext(map)`, NOT from advisor params; tool results live in
    `ToolResponseMessage.getResponses()[].responseData()` (message text is empty).
  - `ChatClient.user(String)` requires non-blank text (`Assert.hasText` throws).
- **Bean cycles**: `AgentRegistryService` resolves `ObjectProvider<List<Agent>>` in
  `afterPropertiesSet`; `InProcessA2aClient` resolves `ObjectProvider<AgentRuntime>`
  at call time. Keep this pattern when adding agents that depend on registry/runtime.
- **Kafka-ready**: `AgentEventBus` port + `InMemoryAgentEventBus` is the seam for the
  future async mode (roadmap in README).

## Configuration

- `application.yml`: no secrets — env placeholders only (`${ANTHROPIC_AUTH_TOKEN:}` etc.);
  presets, guardrail rules, system prompts, H2 file location (`./data/`).
- `.env` (gitignored) holds real credentials; `.sh` scripts source it.
- Test profiles: `mock` (default, scripted `MockChatModel` — `@Profile("mock")`,
  `@Primary` over the real models) and `it` (real LLM). Cucumber context profile comes
  from `-Dcucumber.profile`, surefire sets `spring.profiles.active` via the `it` Maven
  profile; the `-Pit` run only executes `RunCucumberTest` with `@it` tags.

## Testing / coverage rules

- BDD (Cucumber + cucumber-spring): features in `app/src/test/resources/features/mock|it`.
  `MockChatModel` is a scripted queue (text responses or `TOOL_CALLS` tool-call
  responses; records every `Prompt` for assertions). Spring context is shared across
  scenarios — `CommonSteps` `@Before` resets the mock and restarts stopped agents.
- JaCoCo enforces **100% line coverage** on `com.hepexta.allagents.*` (app + core).
  Core classes are copied into `app/target/classes` at verify (jacoco 0.8.13 dropped
  `classDirectories`); exclusions use class-file path patterns (`**/AllAgentsApplication*`,
  `**/config/*`) — dotted package patterns do NOT work. New code must be fully covered
  or the build fails — write tests alongside.
- Boot 4 test artifacts: `@AutoConfigureMockMvc` lives in
  `org.springframework.boot.webmvc.test.autoconfigure` (dependency
  `spring-boot-starter-webmvc-test`); Cucumber suite needs `junit-platform-suite`.
