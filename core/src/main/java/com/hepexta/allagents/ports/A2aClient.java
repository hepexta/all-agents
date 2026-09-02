package com.hepexta.allagents.ports;

import com.hepexta.allagents.domain.a2a.AgentMessage;

public interface A2aClient {

    AgentMessage sendMessage(String agentName, String message, String contextId);
}
