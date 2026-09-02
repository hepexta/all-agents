package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.agent.Agent;
import com.hepexta.allagents.domain.agent.AgentCard;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.a2a.AgentMessage;
import com.hepexta.allagents.domain.a2a.JsonRpcRequest;
import com.hepexta.allagents.domain.a2a.JsonRpcResponse;
import com.hepexta.allagents.domain.a2a.Task;
import com.hepexta.allagents.application.TaskStore;
import com.hepexta.allagents.exception.AgentNotFoundException;
import com.hepexta.allagents.ports.AgentRegistry;
import com.hepexta.allagents.ports.AgentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A2A (Agent2Agent) protocol endpoint: agent cards + JSON-RPC 2.0 methods
 * message/send, message/stream (sync fallback) and tasks/get.
 */
@RestController
@RequestMapping("/a2a")
public class A2aController {

    private static final Logger log = LoggerFactory.getLogger(A2aController.class);

    private final AgentRegistry registry;
    private final AgentRuntime runtime;
    private final TaskStore taskStore;
    private final AppProperties properties;

    public A2aController(AgentRegistry registry, AgentRuntime runtime, TaskStore taskStore, AppProperties properties) {
        this.registry = registry;
        this.runtime = runtime;
        this.taskStore = taskStore;
        this.properties = properties;
    }

    @GetMapping("/agents/{name}")
    public AgentCard card(@PathVariable String name) {
        Agent agent = registry.findByName(name)
                .orElseThrow(() -> new AgentNotFoundException(name));
        return AgentCard.fromDefinition(agent.definition(), cardUrl(name));
    }

    @PostMapping("/agents/{name}")
    public JsonRpcResponse rpc(@PathVariable String name, @RequestBody JsonRpcRequest request) {
        if (registry.findByName(name).isEmpty()) {
            return JsonRpcResponse.error(request.id(), -32001, "agent not found: " + name);
        }
        return switch (request.method()) {
            case "message/send", "message/stream" -> handleSend(name, request);
            case "tasks/get" -> handleTasksGet(request);
            default -> JsonRpcResponse.error(request.id(), -32601, "method not found: " + request.method());
        };
    }

    private JsonRpcResponse handleSend(String name, JsonRpcRequest request) {
        String contextId = contextId(request.params());
        String taskId = UUID.randomUUID().toString();
        taskStore.put(Task.working(taskId, contextId));
        try {
            String instruction = extractText(request.params());
            AgentResult result = runtime.executeByName(name, new AgentRequest(instruction, Map.of(), contextId));
            AgentMessage reply = AgentMessage.of(UUID.randomUUID().toString(), "agent", result.content());
            taskStore.put(taskStore.get(taskId).orElseThrow().completed(reply));
            return JsonRpcResponse.ok(request.id(), reply);
        } catch (RuntimeException e) {
            // Log details server-side; clients only get a generic failure message.
            log.error("A2A message/send failed for agent {}", name, e);
            AgentMessage errorMessage = AgentMessage.of(UUID.randomUUID().toString(), "agent", "agent execution failed");
            taskStore.put(taskStore.get(taskId).orElseThrow().failed(errorMessage));
            return JsonRpcResponse.error(request.id(), -32000, "agent execution failed");
        }
    }

    private JsonRpcResponse handleTasksGet(JsonRpcRequest request) {
        Map<String, Object> params = request.params();
        if (params == null) {
            return JsonRpcResponse.error(request.id(), -32602, "missing params");
        }
        String taskId = String.valueOf(params.get("id"));
        Optional<Task> task = taskStore.get(taskId);
        return task.map(t -> JsonRpcResponse.ok(request.id(), t))
                .orElseGet(() -> JsonRpcResponse.error(request.id(), -32002, "task not found: " + taskId));
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> params) {
        if (params == null || params.get("message") == null) {
            throw new IllegalArgumentException("params.message is required");
        }
        Map<String, Object> message = (Map<String, Object>) params.get("message");
        Object parts = message.get("parts");
        if (!(parts instanceof List<?> partList)) {
            throw new IllegalArgumentException("message.parts is required");
        }
        StringBuilder text = new StringBuilder();
        for (Object part : partList) {
            if (part instanceof Map<?, ?> partMap && "text".equals(partMap.get("kind"))) {
                text.append(partMap.get("text"));
            }
        }
        return text.toString();
    }

    private String contextId(Map<String, Object> params) {
        if (params == null || params.get("contextId") == null) {
            return "";
        }
        return String.valueOf(params.get("contextId"));
    }

    private String cardUrl(String name) {
        return properties.a2a().baseUrl() + "/a2a/agents/" + name;
    }
}
