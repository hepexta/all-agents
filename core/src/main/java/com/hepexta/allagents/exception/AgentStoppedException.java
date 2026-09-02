package com.hepexta.allagents.exception;

import com.hepexta.allagents.domain.agent.AgentId;

public class AgentStoppedException extends RuntimeException {

    public AgentStoppedException(AgentId id) {
        super("agent is stopped: " + id.value());
    }
}
