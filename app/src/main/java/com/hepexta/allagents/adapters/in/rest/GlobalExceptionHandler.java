package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.exception.AgentExecutionException;
import com.hepexta.allagents.exception.AgentNotFoundException;
import com.hepexta.allagents.exception.AgentStoppedException;
import com.hepexta.allagents.exception.ConversationNotFoundException;
import com.hepexta.allagents.exception.GuardrailBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(String code, String message) {
    }

    @ExceptionHandler(AgentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse agentNotFound(AgentNotFoundException e) {
        return new ErrorResponse("agent_not_found", e.getMessage());
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse conversationNotFound(ConversationNotFoundException e) {
        return new ErrorResponse("conversation_not_found", e.getMessage());
    }

    @ExceptionHandler(AgentStoppedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse agentStopped(AgentStoppedException e) {
        return new ErrorResponse("agent_stopped", e.getMessage());
    }

    @ExceptionHandler(GuardrailBlockedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse guardrailBlocked(GuardrailBlockedException e) {
        return new ErrorResponse("guardrail_blocked", e.getMessage());
    }

    @ExceptionHandler(AgentExecutionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse agentExecutionFailed(AgentExecutionException e) {
        return new ErrorResponse("agent_execution_failed", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse illegalState(IllegalStateException e) {
        return new ErrorResponse("illegal_state", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse internal(Exception e) {
        // Log the full exception server-side; never expose its message to the client.
        log.error("Unhandled exception", e);
        return new ErrorResponse("internal_error", "an unexpected error occurred");
    }
}
