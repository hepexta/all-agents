package com.hepexta.allagents.domain.agent;

import java.util.Map;

public record AgentResult(AgentId agentId, String content, Map<String, Object> data) {

    public AgentResult(AgentId agentId, String content) {
        this(agentId, content, Map.of());
    }
}
