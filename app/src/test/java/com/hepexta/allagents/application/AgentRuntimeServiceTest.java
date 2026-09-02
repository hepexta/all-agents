package com.hepexta.allagents.application;

import com.hepexta.allagents.adapters.out.bus.InMemoryAgentEventBus;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.domain.message.AgentEventType;
import com.hepexta.allagents.exception.AgentNotFoundException;
import com.hepexta.allagents.exception.AgentStoppedException;
import com.hepexta.allagents.exception.GuardrailBlockedException;
import com.hepexta.allagents.guardrail.Guardrail;
import com.hepexta.allagents.support.Registries;
import com.hepexta.allagents.support.StubAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentRuntimeServiceTest {

    private AgentRegistryService registry;
    private InMemoryAgentEventBus bus;
    private AgentRuntimeService runtime;

    private static final Guardrail GUARDRAIL = new Guardrail() {
        @Override
        public GuardrailResult checkInput(String input) {
            return input.contains("inject") ? GuardrailResult.block("blocked input") : GuardrailResult.pass(input);
        }

        @Override
        public GuardrailResult checkOutput(String output) {
            if (output.contains("forbid")) {
                return GuardrailResult.block("blocked output");
            }
            if (output.contains("secret")) {
                return GuardrailResult.redact(output.replace("secret", "masked"), "redacted");
            }
            return GuardrailResult.pass(output);
        }
    };

    @BeforeEach
    void setUp() {
        registry = Registries.of(new StubAgent("a", "plain response"));
        bus = new InMemoryAgentEventBus();
        runtime = new AgentRuntimeService(registry, bus, GUARDRAIL);
    }

    @Test
    void successfulExecutionPublishesCompletedEvent() {
        AgentResult result = runtime.executeByName("a", new AgentRequest("do it", Map.of(), "corr-1"));
        assertEquals("plain response", result.content());
        assertEquals(AgentEventType.REQUEST_COMPLETED, bus.history().getLast().type());
        assertEquals("corr-1", bus.history().getLast().correlationId());
    }

    @Test
    void outputIsRedacted() {
        registry = Registries.of(new StubAgent("a", "the secret value"));
        runtime = new AgentRuntimeService(registry, bus, GUARDRAIL);
        AgentResult result = runtime.executeByName("a", new AgentRequest("do it"));
        assertEquals("the masked value", result.content());
    }

    @Test
    void blockedInputThrowsAndPublishesFailedEvent() {
        assertThrows(GuardrailBlockedException.class,
                () -> runtime.executeByName("a", new AgentRequest("inject now")));
        assertEquals(AgentEventType.REQUEST_FAILED, bus.history().getLast().type());
        assertEquals("blocked input", bus.history().getLast().detail());
    }

    @Test
    void blockedOutputThrowsAndPublishesFailedEvent() {
        registry = Registries.of(new StubAgent("a", "forbid this"));
        runtime = new AgentRuntimeService(registry, bus, GUARDRAIL);
        assertThrows(GuardrailBlockedException.class, () -> runtime.executeByName("a", new AgentRequest("do it")));
        assertEquals(AgentEventType.REQUEST_FAILED, bus.history().getLast().type());
        assertEquals("blocked by guardrail: blocked output", bus.history().getLast().detail());
    }

    @Test
    void stoppedAgentThrowsAndPublishesFailedEvent() {
        registry.findByName("a").orElseThrow().stop();
        assertThrows(AgentStoppedException.class, () -> runtime.executeByName("a", new AgentRequest("do it")));
        assertEquals(AgentEventType.REQUEST_FAILED, bus.history().getLast().type());
    }

    @Test
    void unknownAgentThrows() {
        assertThrows(AgentNotFoundException.class, () -> runtime.executeByName("nope", new AgentRequest("do")));
        assertThrows(AgentNotFoundException.class, () -> runtime.execute(new AgentId("nope"), new AgentRequest("do")));
    }

    @Test
    void executeByIdResolvesAgent() {
        AgentResult result = runtime.execute(new AgentId("a"), new AgentRequest("do it"));
        assertEquals("plain response", result.content());
    }

    @Test
    void nullInstructionIsCheckedAsEmptyInput() {
        AgentResult result = runtime.executeByName("a", new AgentRequest(null));
        assertEquals("plain response", result.content());
    }
}
