package com.hepexta.allagents.adapters.out.a2a;

import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.a2a.AgentMessage;
import com.hepexta.allagents.ports.A2aClient;
import com.hepexta.allagents.ports.AgentRuntime;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;
import java.util.UUID;

/**
 * A2A client that dispatches directly to the in-JVM agent runtime
 * (default mode, app.a2a.mode=in-process). The runtime is resolved lazily
 * to break the master-agent -> tools -> runtime -> registry dependency cycle.
 */
public class InProcessA2aClient implements A2aClient {

    private final ObjectProvider<AgentRuntime> runtimeProvider;

    public InProcessA2aClient(ObjectProvider<AgentRuntime> runtimeProvider) {
        this.runtimeProvider = runtimeProvider;
    }

    @Override
    public AgentMessage sendMessage(String agentName, String message, String contextId) {
        AgentResult result = runtimeProvider.getObject()
                .executeByName(agentName, new AgentRequest(message, Map.of(), contextId));
        return AgentMessage.of(UUID.randomUUID().toString(), "agent", result.content());
    }
}
