package com.hepexta.allagents.application;

import com.hepexta.allagents.adapters.out.bus.InMemoryAgentEventBus;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.domain.message.AgentEventType;
import com.hepexta.allagents.exception.AgentNotFoundException;
import com.hepexta.allagents.support.Registries;
import com.hepexta.allagents.support.StubAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentLifecycleServiceTest {

    private AgentRegistryService registry;
    private InMemoryAgentEventBus bus;
    private AgentLifecycleService service;

    @BeforeEach
    void setUp() {
        registry = Registries.of(new StubAgent("a", "ra"), new StubAgent("b", "rb"));
        bus = new InMemoryAgentEventBus();
        service = new AgentLifecycleService(registry, bus);
    }

    @Test
    void statusByIdAndByName() {
        assertEquals(AgentStatus.STARTED, service.status(new AgentId("a")));
        assertEquals(AgentStatus.STARTED, service.statusByName("b"));
    }

    @Test
    void statusOfUnknownAgentThrows() {
        assertThrows(AgentNotFoundException.class, () -> service.status(new AgentId("nope")));
        assertThrows(AgentNotFoundException.class, () -> service.statusByName("nope"));
    }

    @Test
    void stopAndStartPublishEvents() {
        assertEquals(AgentStatus.STOPPED, service.stop(new AgentId("a")));
        assertEquals(AgentEventType.AGENT_STOPPED, bus.history().getLast().type());
        assertEquals("a", bus.history().getLast().agentName());

        assertEquals(AgentStatus.STARTED, service.start(new AgentId("a")));
        assertEquals(AgentEventType.AGENT_STARTED, bus.history().getLast().type());
        assertEquals("a", bus.history().getLast().agentName());
    }

    @Test
    void startAndStopOfUnknownAgentThrow() {
        assertThrows(AgentNotFoundException.class, () -> service.start(new AgentId("nope")));
        assertThrows(AgentNotFoundException.class, () -> service.stop(new AgentId("nope")));
    }

    @Test
    void statusesReturnsAllAgents() {
        assertEquals(2, service.statuses().size());
        assertEquals(AgentStatus.STARTED, service.statuses().get("a"));
    }
}
