package com.hepexta.allagents.domain;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.chat.ConversationId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainValidationTest {

    @Test
    void agentIdRejectsBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new AgentId(null));
        assertThrows(IllegalArgumentException.class, () -> new AgentId(" "));
        assertEquals("ok", new AgentId("ok").value());
    }

    @Test
    void conversationIdRejectsBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new ConversationId(null));
        assertThrows(IllegalArgumentException.class, () -> new ConversationId(""));
        assertEquals("ok", new ConversationId("ok").value());
    }
}
