package com.hepexta.allagents.agents.master;

import com.hepexta.allagents.agent.Agent;
import com.hepexta.allagents.domain.agent.AgentDefinition;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.domain.a2a.AgentMessage;
import com.hepexta.allagents.ports.A2aClient;
import com.hepexta.allagents.ports.AgentLifecycleManager;
import com.hepexta.allagents.ports.AgentRegistry;
import com.hepexta.allagents.tools.CurrentDateTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MasterTools {

    private final A2aClient a2aClient;
    private final AgentRegistry registry;
    private final AgentLifecycleManager lifecycleManager;
    private final CurrentDateTool currentDateTool;

    public MasterTools(A2aClient a2aClient,
                       AgentRegistry registry,
                       AgentLifecycleManager lifecycleManager,
                       CurrentDateTool currentDateTool) {
        this.a2aClient = a2aClient;
        this.registry = registry;
        this.lifecycleManager = lifecycleManager;
        this.currentDateTool = currentDateTool;
    }

    @Tool(description = "Returns the current date and time in ISO-8601 format. Use it when the current date or time is relevant to the request.")
    public String getCurrentDate() {
        return currentDateTool.getCurrentDate();
    }

    @Tool(description = "Lists all registered agents with their descriptions, capabilities and skills.")
    public List<AgentDefinition> listAgents() {
        return registry.all().stream().map(Agent::definition).toList();
    }

    @Tool(description = "Returns the lifecycle status (STARTED or STOPPED) of an agent.")
    public AgentStatus getAgentStatus(@ToolParam(description = "Name of the agent") String agentName) {
        return lifecycleManager.statusByName(agentName);
    }

    @Tool(description = "Starts a previously stopped agent.")
    public AgentStatus startAgent(@ToolParam(description = "Name of the agent") String agentName) {
        return lifecycleManager.start(new AgentId(agentName));
    }

    @Tool(description = "Stops a running agent.")
    public AgentStatus stopAgent(@ToolParam(description = "Name of the agent") String agentName) {
        return lifecycleManager.stop(new AgentId(agentName));
    }

    @Tool(description = "Delegates a task to a specialist agent over the A2A protocol and returns that agent's answer.")
    public String delegateToAgent(@ToolParam(description = "Name of the specialist agent, e.g. pdf-extractor") String agentName,
                                  @ToolParam(description = "Instruction for the specialist agent") String instruction,
                                  @ToolParam(description = "Optional conversation context id", required = false) String contextId) {
        AgentMessage response = a2aClient.sendMessage(agentName, instruction, contextId);
        return response.text();
    }
}
