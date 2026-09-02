package com.hepexta.allagents.adapters.out.a2a;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.ports.AgentRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InProcessA2aClientTest {

    @Test
    void sendMessageDelegatesToRuntimeAndWrapsResult() {
        AgentRuntime runtime = new AgentRuntime() {
            @Override
            public AgentResult execute(AgentId id, AgentRequest request) {
                return new AgentResult(id, "hello from " + id.value());
            }

            @Override
            public AgentResult executeByName(String name, AgentRequest request) {
                return new AgentResult(new AgentId(name), "hello from " + name);
            }
        };
        InProcessA2aClient client = new InProcessA2aClient(new ObjectProvider<>() {
            @Override
            public AgentRuntime getObject(Object... args) throws BeansException {
                return runtime;
            }

            @Override
            public AgentRuntime getIfAvailable() throws BeansException {
                return runtime;
            }

            @Override
            public AgentRuntime getIfUnique() throws BeansException {
                return runtime;
            }

            @Override
            public AgentRuntime getObject() throws BeansException {
                return runtime;
            }
        });
        var message = client.sendMessage("pdf-extractor", "extract", "ctx-1");
        assertEquals("agent", message.role());
        assertEquals("hello from pdf-extractor", message.text());
        assertNotNull(message.messageId());
    }
}
