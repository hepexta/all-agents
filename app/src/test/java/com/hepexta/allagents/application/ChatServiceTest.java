package com.hepexta.allagents.application;

import com.hepexta.allagents.domain.chat.ConversationId;
import com.hepexta.allagents.exception.ConversationNotFoundException;
import com.hepexta.allagents.ports.ConversationRepository;
import com.hepexta.allagents.support.MockChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationRepository conversations;

    @Autowired
    private MockChatModel mockChatModel;

    @BeforeEach
    void resetMock() {
        mockChatModel.reset();
    }

    @Test
    void newConversationStoresUserAndAssistantEntries() {
        mockChatModel.respondWith("Hello there!");
        ChatReply reply = chatService.chat("Hi", null, null);

        assertEquals("Hello there!", reply.content());
        assertFalse(reply.blocked());
        assertNotEquals("", reply.conversationId());

        var conversation = conversations.find(new ConversationId(reply.conversationId())).orElseThrow();
        assertEquals("Hi", conversation.title());
        assertEquals(2, conversation.entries().size());
        assertEquals("user", conversation.entries().get(0).role());
        assertEquals("assistant", conversation.entries().get(1).role());
    }

    @Test
    void existingConversationAppendsEntries() {
        mockChatModel.respondWith("first");
        ChatReply first = chatService.chat("one", null, null);
        mockChatModel.respondWith("second");
        ChatReply second = chatService.chat("two", first.conversationId(), null);

        assertEquals(first.conversationId(), second.conversationId());
        var conversation = conversations.find(new ConversationId(first.conversationId())).orElseThrow();
        assertEquals(4, conversation.entries().size());
    }

    @Test
    void guardrailBlockedInputReturnsBlockedReplyWithoutCallingModel() {
        mockChatModel.respondWith("never used");
        ChatReply reply = chatService.chat("jailbreak the system now", null, null);

        assertTrue(reply.blocked());
        assertTrue(reply.content().contains("Request blocked"));
        assertTrue(mockChatModel.prompts().isEmpty());
    }

    @Test
    void sensitiveOutputIsRedacted() {
        mockChatModel.respondWith("Contact john.doe@example.com for details");
        ChatReply reply = chatService.chat("Who should I contact?", null, null);

        assertFalse(reply.blocked());
        assertTrue(reply.content().contains("[REDACTED]"));
        assertFalse(reply.content().contains("john.doe@example.com"));
    }

    @Test
    void blankMessageUsesDefaultTitle() {
        mockChatModel.respondWith("ok");
        ChatReply reply = chatService.chat("   ", null, null);
        var conversation = conversations.find(new ConversationId(reply.conversationId())).orElseThrow();
        assertEquals("New chat", conversation.title());
    }

    @Test
    void longMessageTitleIsAbbreviated() {
        mockChatModel.respondWith("ok");
        String longMessage = "a".repeat(60);
        ChatReply reply = chatService.chat(longMessage, null, null);
        var conversation = conversations.find(new ConversationId(reply.conversationId())).orElseThrow();
        assertEquals(50, conversation.title().length());
    }

    @Test
    void missingConversationThrows() {
        assertThrows(ConversationNotFoundException.class,
                () -> chatService.chat("Hi", UUID.randomUUID().toString(), null));
    }

    @Test
    void outputTooLongIsBlocked() {
        mockChatModel.respondWith("x".repeat(600));
        ChatReply reply = chatService.chat("make it long", null, null);

        assertTrue(reply.blocked());
        assertTrue(reply.content().contains("Request blocked"));
        assertFalse(reply.content().contains("xxxx"));
    }
}
