package com.hepexta.allagents.application;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.support.FixedObjectProvider;
import com.hepexta.allagents.support.StubAgent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRegistryServiceTest {

    private AgentRegistryService readyRegistry() {
        AgentRegistryService registry = new AgentRegistryService(new FixedObjectProvider<>(
                List.of(new StubAgent("a", "ra"), new StubAgent("b", "rb"))));
        registry.afterSingletonsInstantiated();
        return registry;
    }

    @Test
    void afterPropertiesSetRegistersAndStartsAllAgents() {
        AgentRegistryService registry = readyRegistry();
        assertEquals(2, registry.all().size());
        registry.all().forEach(agent ->
                assertEquals(com.hepexta.allagents.domain.agent.AgentStatus.STARTED, agent.status()));
    }

    @Test
    void findByIdAndByName() {
        AgentRegistryService registry = readyRegistry();
        assertTrue(registry.find(new AgentId("a")).isPresent());
        assertTrue(registry.findByName("a").isPresent());
        assertTrue(registry.find(new AgentId("nope")).isEmpty());
        assertTrue(registry.findByName("nope").isEmpty());
    }

    @Test
    void duplicateRegistrationThrows() {
        AgentRegistryService registry = readyRegistry();
        assertThrows(IllegalStateException.class, () -> registry.register(new StubAgent("a", "ra2")));
    }

    @Test
    void registerAddsNewAgent() {
        AgentRegistryService registry = new AgentRegistryService(new FixedObjectProvider<>(List.of()));
        registry.register(new StubAgent("new", "r"));
        assertEquals(1, registry.all().size());
    }
}
