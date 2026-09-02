package com.hepexta.allagents.exception;

import com.hepexta.allagents.domain.agent.AgentId;

public class AgentExecutionException extends RuntimeException {

    public AgentExecutionException(AgentId id, Throwable cause) {
        super("agent execution failed: " + id.value(), cause);
    }
}
