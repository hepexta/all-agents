package com.hepexta.allagents.adapters.out.memory;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JdbcChatMemoryTest {

    @Autowired
    private JdbcChatMemory chatMemory;

    @Test
    void addGetAndClearMessages() {
        String conversationId = UUID.randomUUID().toString();
        chatMemory.add(conversationId, List.of(
                new UserMessage("question 1"),
                new AssistantMessage("answer 1"),
                new UserMessage("question 2"),
                new AssistantMessage("answer 2")));

        var messages = chatMemory.get(conversationId);
        assertEquals(4, messages.size());
        assertEquals("question 1", messages.get(0).getText());
        assertEquals("answer 2", messages.get(3).getText());
        assertEquals("user", messages.get(2).getMessageType().name().toLowerCase());

        chatMemory.clear(conversationId);
        assertTrue(chatMemory.get(conversationId).isEmpty());
    }

    @Test
    void messagesAreIsolatedPerConversation() {
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        chatMemory.add(first, List.of(new UserMessage("for first")));
        chatMemory.add(second, List.of(new UserMessage("for second")));

        assertEquals(1, chatMemory.get(first).size());
        assertEquals("for second", chatMemory.get(second).getFirst().getText());
    }
}
