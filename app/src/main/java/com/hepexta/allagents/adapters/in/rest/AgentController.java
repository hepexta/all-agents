package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.agent.AgentCard;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.domain.tool.ToolInfo;
import com.hepexta.allagents.ports.AgentLifecycleManager;
import com.hepexta.allagents.ports.AgentRegistry;
import com.hepexta.allagents.ports.AgentRuntime;
import com.hepexta.allagents.ports.ToolCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final AgentRegistry registry;
    private final AgentLifecycleManager lifecycleManager;
    private final AgentRuntime runtime;
    private final ToolCatalog toolCatalog;
    private final AppProperties properties;

    public AgentController(AgentRegistry registry,
                           AgentLifecycleManager lifecycleManager,
                           AgentRuntime runtime,
                           ToolCatalog toolCatalog,
                           AppProperties properties) {
        this.registry = registry;
        this.lifecycleManager = lifecycleManager;
        this.runtime = runtime;
        this.toolCatalog = toolCatalog;
        this.properties = properties;
    }

    @GetMapping("/agents")
    public List<AgentCard> agents() {
        return registry.all().stream()
                .map(agent -> AgentCard.fromDefinition(agent.definition(), cardUrl(agent.id().value())))
                .toList();
    }

    @PostMapping("/agents/{name}/start")
    public AgentStatus start(@PathVariable String name) {
        return lifecycleManager.start(new AgentId(name));
    }

    @PostMapping("/agents/{name}/stop")
    public AgentStatus stop(@PathVariable String name) {
        return lifecycleManager.stop(new AgentId(name));
    }

    @GetMapping("/agents/{name}/status")
    public AgentStatus status(@PathVariable String name) {
        return lifecycleManager.statusByName(name);
    }

    public record ExecuteRequest(String instruction, Map<String, Object> payload) {
    }

    @PostMapping("/agents/{name}/execute")
    public AgentResult execute(@PathVariable String name, @RequestBody ExecuteRequest request) {
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        return runtime.executeByName(name, new AgentRequest(request.instruction(), payload, null));
    }

    @GetMapping("/tools")
    public List<ToolInfo> tools() {
        return toolCatalog.all();
    }

    private String cardUrl(String name) {
        return properties.a2a().baseUrl() + "/a2a/agents/" + name;
    }
}
