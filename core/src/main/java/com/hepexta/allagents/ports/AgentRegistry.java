package com.hepexta.allagents.ports;

import com.hepexta.allagents.agent.Agent;
import com.hepexta.allagents.domain.agent.AgentId;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {

    void register(Agent agent);

    Optional<Agent> find(AgentId id);

    Optional<Agent> findByName(String name);

    List<Agent> all();
}
