package com.hepexta.allagents.domain.agent;

import java.util.Map;

public record AgentRequest(String instruction, Map<String, Object> payload, String correlationId) {

    public AgentRequest(String instruction) {
        this(instruction, Map.of(), null);
    }
}
