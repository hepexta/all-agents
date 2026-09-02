package com.hepexta.allagents.agents.master;

import com.hepexta.allagents.agent.AbstractAgent;
import com.hepexta.allagents.ai.GuardrailAdvisor;
import com.hepexta.allagents.application.PresetService;
import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.agent.AgentCapability;
import com.hepexta.allagents.domain.agent.AgentDefinition;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.agent.AgentSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.toolsearch.ToolSearchTool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MasterAgent extends AbstractAgent {

    public static final String ID = "master";
    public static final String TOOL_SEARCH_SESSION = "default";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final PresetService presetService;

    public MasterAgent(ChatClient.Builder builder,
                       MasterTools masterTools,
                       GuardrailAdvisor guardrailAdvisor,
                       ChatMemory chatMemory,
                       AppProperties properties,
                       PresetService presetService,
                       ToolSearchTool toolSearchTool) {
        super(agentDefinition());
        this.chatMemory = chatMemory;
        this.presetService = presetService;
        this.chatClient = builder
                .defaultSystem(properties.agents().masterSystemPrompt())
                .defaultTools(masterTools, toolSearchTool)
                .defaultToolContext(Map.of(ToolSearchTool.TOOL_SEARCH_TOOL_SESSION_ID_KEY, TOOL_SEARCH_SESSION))
                .defaultAdvisors(guardrailAdvisor)
                .build();
    }

    private static AgentDefinition agentDefinition() {
        return new AgentDefinition(
                new AgentId(ID),
                "Master Agent",
                "Orchestrates all specialist agents: routes requests, controls agent lifecycle and knows every agent, skill and tool available.",
                List.of(new AgentCapability("orchestration", "Coordinates specialist agents via the A2A protocol")),
                List.of(new AgentSkill("delegation", "Delegation", "Delegate tasks to specialist agents",
                                List.of("Extract data from a PDF", "Ask a specialist to analyze a document")),
                        new AgentSkill("agent-lifecycle", "Agent lifecycle", "Start, stop and inspect agents",
                                List.of("Stop the pdf-extractor", "What is the status of every agent?")),
                        new AgentSkill("tool-discovery", "Tool discovery", "Discover tools and skills via tool search",
                                List.of("Which agent can process images?"))),
                List.of("getCurrentDate", "toolSearchTool", "listAgents", "getAgentStatus", "startAgent", "stopAgent", "delegateToAgent"));
    }

    public String chat(String userMessage, String conversationId, String presetId) {
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec
                        .advisors(memoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .options(ToolCallingChatOptions.builder())
                .user(userMessage);
        Optional<AppProperties.Preset> preset = presetService.find(presetId);
        preset.ifPresent(p -> {
            if (StringUtils.hasText(p.systemPrompt())) {
                spec.system(p.systemPrompt());
            }
            if (p.temperature() != null) {
                spec.options(ToolCallingChatOptions.builder().temperature(p.temperature()));
            }
        });
        return spec.call().content();
    }

    @Override
    protected AgentResult doExecute(AgentRequest request) {
        String content = chatClient.prompt()
                .options(ToolCallingChatOptions.builder())
                .user(request.instruction())
                .call()
                .content();
        return new AgentResult(id(), content);
    }
}
