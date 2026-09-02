# Adding a New (Slave) Agent

Step-by-step guide for integrating the next specialist agent into the platform.
The platform is designed for ~100 agents; every agent follows the same pattern.

## 1. Create the agent package

```
app/src/main/java/com/hepexta/allagents/agents/<agent-name>/
```

## 2. Extend `AbstractAgent`

```java
package com.hepexta.allagents.agents.<agent-name>;

import com.hepexta.allagents.agent.AbstractAgent;
import com.hepexta.allagents.domain.agent.*;
import org.springframework.stereotype.Component;

@Component
public class <Name>Agent extends AbstractAgent {

    public static final String ID = "<agent-name>";

    public <Name>Agent(/* dependencies, e.g. ChatClient.Builder */) {
        super(agentDefinition());
        // build your ChatClient here if the agent needs an LLM
    }

    private static AgentDefinition agentDefinition() {
        return new AgentDefinition(
                new AgentId(ID),
                "<Human readable name>",
                "<What the agent does — shown to the master agent for routing>",
                List.of(new AgentCapability("<capability-id>", "<capability description>")),
                List.of(new AgentSkill("<skill-id>", "<skill name>", "<skill description>",
                        List.of("<example prompt 1>", "<example prompt 2>"))),
                List.of("<tool names this agent exposes>"));   // optional
    }

    @Override
    protected AgentResult doExecute(AgentRequest request) {
        // 1. read request.instruction() and request.payload()
        // 2. do the work (call your ChatClient, external APIs, etc.)
        // 3. return new AgentResult(id(), "<content>", Map.of(...));
        throw new UnsupportedOperationException("implement me");
    }
}
```

Rules:

- `execute()` is guarded by the framework: stopped agents throw `AgentStoppedException`,
  runtime failures are wrapped in `AgentExecutionException`.
- Input/output guardrails are applied centrally by `AgentRuntimeService` — no need to
  duplicate them in the agent.
- `@Component` is all you need for registration: `AgentRegistryService` discovers every
  `Agent` bean, registers it and starts it at boot.

## 3. Optional: give the agent its own tools

Annotate methods with Spring AI `@Tool` and register them via `ChatClient.Builder.defaultTools(...)`
or return their names in `AgentDefinition.toolNames()` so the master agent's tool search
(`toolSearchTool`) and `listAgents` expose them. Register them in the catalog:

```java
// in ToolCatalogRegistrar (or your own registrar):
catalog.register(new ToolInfo("<toolName>", "<description>", ID));
toolIndex.indexTool(ToolCatalogRegistrar.TOOL_SEARCH_SESSION,
        ToolReference.builder().toolName("<toolName>").summary("<description>").build());
```

## 4. Everything else is automatic

- **A2A**: the agent gets an A2A card at `GET /a2a/agents/<agent-name>` and can be called
  via JSON-RPC `message/send` — same endpoint the master agent's `delegateToAgent` tool uses.
- **REST**: `POST /api/agents/<agent-name>/execute`, start/stop/status endpoints.
- **Lifecycle**: master agent tools `startAgent` / `stopAgent` / `getAgentStatus` work
  immediately; lifecycle changes publish events on the `AgentEventBus`.

## 5. Tests

1. Unit tests for the agent's logic (see `PdfExtractionAgentTest`).
2. BDD scenarios in `app/src/test/resources/features/mock/<agent-name>.feature` using the
   scripted `MockChatModel` (default profile, no real LLM):

   ```gherkin
   Feature: <Name> agent (mock LLM)
     Scenario: <happy path>
       Given a pdf with text "..."          # or your own setup step
       And the mock LLM will respond with "..."
       When the <name> agent receives instruction "..."
       Then the result contains "..."
   ```

3. Optionally add `@it` scenarios in `features/it/` for real-LLM runs (`scripts/it-test`).

## 6. Verify

```bash
scripts/test.sh    # all tests incl. your BDD scenarios, mock LLM
scripts/build.sh   # full build with the 100% line-coverage gate
```

New agent code counts toward the coverage gate — cover all branches or the build fails.
