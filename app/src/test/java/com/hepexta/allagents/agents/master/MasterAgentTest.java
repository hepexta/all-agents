package com.hepexta.allagents.agents.master;

import com.hepexta.allagents.adapters.out.memory.JdbcChatMemory;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.support.MockChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class MasterAgentTest {

    @Autowired
    private MasterAgent masterAgent;

    @Autowired
    private JdbcChatMemory chatMemory;

    @Autowired
    private MockChatModel mockChatModel;

    @BeforeEach
    void resetMock() {
        mockChatModel.reset();
        masterAgent.start();
    }

    @Test
    void executeAnswersViaChatClient() {
        mockChatModel.respondWith("orchestrated answer");
        AgentResult result = masterAgent.execute(new AgentRequest("do something"));
        assertEquals("orchestrated answer", result.content());
        assertEquals("master", result.agentId().value());
    }

    @Test
    void chatStoresMessagesInMemory() {
        String conversationId = UUID.randomUUID().toString();
        mockChatModel.respondWith("first answer");
        assertEquals("first answer", masterAgent.chat("first question", conversationId, null));

        assertFalse(chatMemory.get(conversationId).isEmpty());
        assertEquals("first question", chatMemory.get(conversationId).getFirst().getText());
    }

    @Test
    void chatWithDefaultPresetSkipsBlankSystemPrompt() {
        String conversationId = UUID.randomUUID().toString();
        mockChatModel.respondWith("default answer");
        assertEquals("default answer", masterAgent.chat("hello", conversationId, "default"));
    }

    @Test
    void definitionExposesOrchestrationCapabilities() {
        assertEquals("master", masterAgent.id().value());
        assertEquals("Master Agent", masterAgent.definition().name());
        assertEquals(1, masterAgent.definition().capabilities().size());
        assertEquals(3, masterAgent.definition().skills().size());
        assertEquals(7, masterAgent.definition().toolNames().size());
    }
}
