package com.hepexta.allagents.adapters.out.a2a;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hepexta.allagents.domain.a2a.AgentMessage;
import com.hepexta.allagents.domain.a2a.JsonRpcRequest;
import com.hepexta.allagents.domain.a2a.JsonRpcResponse;
import com.hepexta.allagents.ports.A2aClient;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A2A client speaking JSON-RPC 2.0 over HTTP to a remote agent endpoint
 * (app.a2a.mode=http). Used to reach agents running in other processes.
 */
public class HttpA2aClient implements A2aClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpA2aClient(RestClient.Builder builder, String baseUrl, ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentMessage sendMessage(String agentName, String message, String contextId) {
        Map<String, Object> messageParam = Map.of(
                "role", "user",
                "parts", List.of(Map.of("kind", "text", "text", message)));
        JsonRpcRequest request = new JsonRpcRequest(
                "2.0",
                UUID.randomUUID().toString(),
                "message/send",
                Map.of("message", messageParam, "contextId", contextId == null ? "" : contextId));
        JsonRpcResponse response = restClient.post()
                .uri("/a2a/agents/{name}", agentName)
                .body(request)
                .retrieve()
                .body(JsonRpcResponse.class);
        if (response == null || response.result() == null) {
            throw new IllegalStateException("empty A2A response from agent: " + agentName);
        }
        return objectMapper.convertValue(response.result(), AgentMessage.class);
    }
}
