package com.hepexta.allagents.support;

import com.hepexta.allagents.agent.Agent;
import com.hepexta.allagents.application.AgentRegistryService;

import java.util.List;

public final class Registries {

    private Registries() {
    }

    public static AgentRegistryService of(Agent... agents) {
        AgentRegistryService registry = new AgentRegistryService(new FixedObjectProvider<>(List.of(agents)));
        registry.afterSingletonsInstantiated();
        return registry;
    }
}
