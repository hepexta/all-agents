package com.hepexta.allagents.ports;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentStatus;

import java.util.Map;

public interface AgentLifecycleManager {

    AgentStatus status(AgentId id);

    AgentStatus statusByName(String name);

    AgentStatus start(AgentId id);

    AgentStatus stop(AgentId id);

    Map<String, AgentStatus> statuses();
}
