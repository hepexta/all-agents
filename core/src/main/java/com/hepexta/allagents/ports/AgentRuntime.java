package com.hepexta.allagents.ports;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;

public interface AgentRuntime {

    AgentResult execute(AgentId id, AgentRequest request);

    AgentResult executeByName(String name, AgentRequest request);
}
