package com.hepexta.allagents.application;

import com.hepexta.allagents.agent.Agent;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.domain.message.AgentEvent;
import com.hepexta.allagents.domain.message.AgentEventType;
import com.hepexta.allagents.exception.AgentNotFoundException;
import com.hepexta.allagents.ports.AgentEventBus;
import com.hepexta.allagents.ports.AgentLifecycleManager;
import com.hepexta.allagents.ports.AgentRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AgentLifecycleService implements AgentLifecycleManager {

    private final AgentRegistry registry;
    private final AgentEventBus eventBus;

    public AgentLifecycleService(AgentRegistry registry, AgentEventBus eventBus) {
        this.registry = registry;
        this.eventBus = eventBus;
    }

    @Override
    public AgentStatus status(AgentId id) {
        return requireAgent(id).status();
    }

    @Override
    public AgentStatus statusByName(String name) {
        return requireAgent(name).status();
    }

    @Override
    public AgentStatus start(AgentId id) {
        Agent agent = requireAgent(id);
        agent.start();
        eventBus.publish(new AgentEvent(AgentEventType.AGENT_STARTED, agent.id().value(), null, Instant.now(), null));
        return agent.status();
    }

    @Override
    public AgentStatus stop(AgentId id) {
        Agent agent = requireAgent(id);
        agent.stop();
        eventBus.publish(new AgentEvent(AgentEventType.AGENT_STOPPED, agent.id().value(), null, Instant.now(), null));
        return agent.status();
    }

    @Override
    public Map<String, AgentStatus> statuses() {
        Map<String, AgentStatus> statuses = new LinkedHashMap<>();
        registry.all().forEach(agent -> statuses.put(agent.id().value(), agent.status()));
        return statuses;
    }

    private Agent requireAgent(AgentId id) {
        return requireAgent(id.value());
    }

    private Agent requireAgent(String name) {
        return registry.findByName(name)
                .orElseThrow(() -> new AgentNotFoundException(name));
    }
}
