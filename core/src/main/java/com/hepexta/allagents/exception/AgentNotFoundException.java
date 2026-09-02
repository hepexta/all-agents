package com.hepexta.allagents.exception;

public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(String name) {
        super("agent not found: " + name);
    }
}
