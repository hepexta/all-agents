package com.hepexta.allagents.application;

import com.hepexta.allagents.agent.Agent;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.ports.AgentRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of all agents. Agents are resolved after all singletons are
 * instantiated to break the master-agent -> tools -> registry -> master-agent
 * cycle.
 */
@Service
public class AgentRegistryService implements AgentRegistry, SmartInitializingSingleton {

    private final Map<String, Agent> agents = new LinkedHashMap<>();
    private final ObjectProvider<List<Agent>> agentsProvider;

    public AgentRegistryService(ObjectProvider<List<Agent>> agentsProvider) {
        this.agentsProvider = agentsProvider;
    }

    @Override
    public void afterSingletonsInstantiated() {
        agentsProvider.getObject().forEach(this::register);
    }

    @Override
    public void register(Agent agent) {
        if (agents.containsKey(agent.id().value())) {
            throw new IllegalStateException("agent already registered: " + agent.id().value());
        }
        agents.put(agent.id().value(), agent);
        agent.start();
    }

    @Override
    public Optional<Agent> find(AgentId id) {
        return Optional.ofNullable(agents.get(id.value()));
    }

    @Override
    public Optional<Agent> findByName(String name) {
        return Optional.ofNullable(agents.get(name));
    }

    @Override
    public List<Agent> all() {
        return List.copyOf(agents.values());
    }
}
