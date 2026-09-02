package com.hepexta.allagents.agents.postgres;

import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.exception.AgentStoppedException;
import com.hepexta.allagents.support.MockChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PostgresExpertAgentTest {

    @Autowired
    private PostgresExpertAgent agent;

    @Autowired
    private MockChatModel mockChatModel;

    @BeforeEach
    void resetMock() {
        mockChatModel.reset();
        agent.start();
    }

    @Test
    void loadsReferenceTopicViaToolThenAnswers() {
        mockChatModel.toolCall("loadReference", "{\"topic\":\"schema-design\"}");
        mockChatModel.respondWith("Use BIGINT GENERATED ALWAYS AS IDENTITY for primary keys.");

        AgentResult result = agent.execute(new AgentRequest("How should I design primary keys?"));

        assertEquals("Use BIGINT GENERATED ALWAYS AS IDENTITY for primary keys.", result.content());
        assertEquals("postgres-expert", result.agentId().value());
        assertTrue(toolResultTexts().stream().anyMatch(text -> text.contains("BIGINT GENERATED ALWAYS AS IDENTITY")),
                "the LLM never received the loaded reference as a tool result");
    }

    @Test
    void answersDirectlyWithoutToolCalls() {
        mockChatModel.respondWith("VACUUM reclaims dead tuples.");

        AgentResult result = agent.execute(new AgentRequest("What does VACUUM do?"));

        assertEquals("VACUUM reclaims dead tuples.", result.content());
        assertEquals(1, mockChatModel.prompts().size());
    }

    @Test
    void failedToolExecutionContinuesTheLoopWithErrorResponse() {
        mockChatModel.toolCall("loadReference", "{\"topic\":\"nope\"}");
        mockChatModel.respondWith("I could not load that topic.");

        AgentResult result = agent.execute(new AgentRequest("Explain primary keys"));

        assertEquals("I could not load that topic.", result.content());
        assertTrue(toolResultTexts().stream().anyMatch(text -> text.contains("unknown topic: nope")),
                "the LLM never received the tool error as a tool result");
    }

    @Test
    void stoppedAgentRefusesRequests() {
        agent.stop();
        assertThrows(AgentStoppedException.class, () -> agent.execute(new AgentRequest("Explain indexes")));
    }

    @Test
    void definitionExposesPostgresCapabilities() {
        assertEquals("postgres-expert", agent.id().value());
        assertEquals("Postgres Expert", agent.definition().name());
        assertEquals(3, agent.definition().capabilities().size());
        assertEquals(3, agent.definition().skills().size());
        assertEquals(List.of("loadReference"), agent.definition().toolNames());
    }

    private List<String> toolResultTexts() {
        return mockChatModel.prompts().stream()
                .map(Prompt::getInstructions)
                .flatMap(List::stream)
                .filter(message -> message.getMessageType() == MessageType.TOOL)
                .map(message -> (ToolResponseMessage) message)
                .flatMap(toolResponse -> toolResponse.getResponses().stream())
                .map(ToolResponseMessage.ToolResponse::responseData)
                .toList();
    }
}
