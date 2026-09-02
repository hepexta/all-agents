package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.chat.ConversationId;
import com.hepexta.allagents.exception.AgentExecutionException;
import com.hepexta.allagents.exception.AgentNotFoundException;
import com.hepexta.allagents.exception.AgentStoppedException;
import com.hepexta.allagents.exception.ConversationNotFoundException;
import com.hepexta.allagents.exception.GuardrailBlockedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void agentNotFound() {
        var response = handler.agentNotFound(new AgentNotFoundException("x"));
        assertEquals("agent_not_found", response.code());
    }

    @Test
    void conversationNotFound() {
        var response = handler.conversationNotFound(new ConversationNotFoundException(new ConversationId("c")));
        assertEquals("conversation_not_found", response.code());
    }

    @Test
    void agentStopped() {
        var response = handler.agentStopped(new AgentStoppedException(new AgentId("a")));
        assertEquals("agent_stopped", response.code());
    }

    @Test
    void guardrailBlocked() {
        var response = handler.guardrailBlocked(new GuardrailBlockedException("reason"));
        assertEquals("guardrail_blocked", response.code());
    }

    @Test
    void agentExecutionFailed() {
        var response = handler.agentExecutionFailed(new AgentExecutionException(new AgentId("a"), new RuntimeException()));
        assertEquals("agent_execution_failed", response.code());
    }

    @Test
    void illegalState() {
        var response = handler.illegalState(new IllegalStateException("dup"));
        assertEquals("illegal_state", response.code());
    }

    @Test
    void internalErrorDoesNotLeakExceptionDetails() {
        var response = handler.internal(new RuntimeException("boom: secret internals"));
        assertEquals("internal_error", response.code());
        assertEquals("an unexpected error occurred", response.message());
    }
}
