package com.hepexta.allagents.support;

import com.hepexta.allagents.agent.AbstractAgent;
import com.hepexta.allagents.domain.agent.AgentDefinition;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;

import java.util.List;

public class StubAgent extends AbstractAgent {

    private final String response;
    private final RuntimeException failure;

    public StubAgent(String id, String response, RuntimeException failure) {
        super(new AgentDefinition(new AgentId(id), id, "stub agent", List.of(), List.of(), List.of()));
        this.response = response;
        this.failure = failure;
    }

    public StubAgent(String id, String response) {
        this(id, response, null);
    }

    @Override
    protected AgentResult doExecute(AgentRequest request) {
        if (failure != null) {
            throw failure;
        }
        return new AgentResult(id(), response);
    }
}
