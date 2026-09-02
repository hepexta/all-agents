package com.hepexta.allagents.agent;

import com.hepexta.allagents.domain.agent.AgentDefinition;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.agent.AgentStatus;

public interface Agent {

    AgentId id();

    AgentDefinition definition();

    AgentStatus status();

    void start();

    void stop();

    AgentResult execute(AgentRequest request);
}
