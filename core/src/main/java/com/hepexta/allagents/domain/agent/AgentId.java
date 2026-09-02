package com.hepexta.allagents.domain.agent;

import com.fasterxml.jackson.annotation.JsonValue;

public record AgentId(@JsonValue String value) {

    public AgentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("agent id must not be blank");
        }
    }
}
