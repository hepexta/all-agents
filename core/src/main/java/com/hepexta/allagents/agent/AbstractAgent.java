package com.hepexta.allagents.agent;

import com.hepexta.allagents.domain.agent.AgentDefinition;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.exception.AgentExecutionException;
import com.hepexta.allagents.exception.AgentStoppedException;

public abstract class AbstractAgent implements Agent {

    private final AgentDefinition definition;
    private volatile AgentStatus status = AgentStatus.STOPPED;

    protected AbstractAgent(AgentDefinition definition) {
        this.definition = definition;
    }

    @Override
    public AgentId id() {
        return definition.id();
    }

    @Override
    public AgentDefinition definition() {
        return definition;
    }

    @Override
    public AgentStatus status() {
        return status;
    }

    @Override
    public void start() {
        status = AgentStatus.STARTED;
    }

    @Override
    public void stop() {
        status = AgentStatus.STOPPED;
    }

    @Override
    public final AgentResult execute(AgentRequest request) {
        if (status != AgentStatus.STARTED) {
            throw new AgentStoppedException(definition.id());
        }
        try {
            return doExecute(request);
        } catch (AgentStoppedException | AgentExecutionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AgentExecutionException(definition.id(), e);
        }
    }

    protected abstract AgentResult doExecute(AgentRequest request);
}
