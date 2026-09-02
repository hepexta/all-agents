package com.hepexta.allagents.agents.master;

import com.hepexta.allagents.adapters.out.bus.InMemoryAgentEventBus;
import com.hepexta.allagents.application.AgentLifecycleService;
import com.hepexta.allagents.application.AgentRegistryService;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.domain.a2a.AgentMessage;
import com.hepexta.allagents.ports.A2aClient;
import com.hepexta.allagents.support.Registries;
import com.hepexta.allagents.support.StubAgent;
import com.hepexta.allagents.tools.CurrentDateTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasterToolsTest {

    private MasterTools tools;
    private AgentRegistryService registry;

    @BeforeEach
    void setUp() {
        registry = Registries.of(new StubAgent("pdf-extractor", "pdf result"));
        A2aClient a2aClient = (agentName, message, contextId) ->
                AgentMessage.of("m-1", "agent", "answer from " + agentName + ": " + message);
        tools = new MasterTools(
                a2aClient,
                registry,
                new AgentLifecycleService(registry, new InMemoryAgentEventBus()),
                new CurrentDateTool(() -> LocalDateTime.of(2026, 8, 31, 10, 0)));
    }

    @Test
    void getCurrentDateReturnsIsoDate() {
        assertEquals("2026-08-31T10:00:00", tools.getCurrentDate());
    }

    @Test
    void listAgentsReturnsDefinitions() {
        assertEquals(1, tools.listAgents().size());
        assertEquals("pdf-extractor", tools.listAgents().getFirst().id().value());
    }

    @Test
    void getAgentStatusReturnsStatus() {
        assertEquals(AgentStatus.STARTED, tools.getAgentStatus("pdf-extractor"));
    }

    @Test
    void startAndStopAgentChangeStatus() {
        tools.stopAgent("pdf-extractor");
        assertEquals(AgentStatus.STOPPED, tools.getAgentStatus("pdf-extractor"));
        tools.startAgent("pdf-extractor");
        assertEquals(AgentStatus.STARTED, tools.getAgentStatus("pdf-extractor"));
    }

    @Test
    void delegateToAgentReturnsAgentAnswer() {
        String answer = tools.delegateToAgent("pdf-extractor", "extract this", "ctx-1");
        assertEquals("answer from pdf-extractor: extract this", answer);
    }
}
