package com.hepexta.allagents.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Scripted ChatModel for tests: responses are queued in advance and consumed
 * in order, including the tool-calling loop (a TOOL_CALLS response followed by
 * the final text response). All received prompts are recorded for assertions.
 */
public class MockChatModel implements ChatModel {

    private final Deque<ChatResponse> script = new ArrayDeque<>();
    private final List<Prompt> prompts = new ArrayList<>();

    public void reset() {
        script.clear();
        prompts.clear();
    }

    public void respondWith(String text) {
        script.add(textResponse(text));
    }

    public void toolCall(String toolName, String argumentsJson) {
        script.add(toolCallResponse(toolName, argumentsJson));
    }

    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        ChatResponse next = script.poll();
        if (next == null) {
            throw new IllegalStateException("mock LLM script exhausted, received prompt: " + prompt);
        }
        return next;
    }

    /**
     * Prompt options are derived from the model's options type; ToolCallingChatOptions
     * is required for the ChatClient tool-calling loop to run.
     */
    @Override
    public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
        return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
    }

    public static ChatResponse textResponse(String text) {
        Generation generation = new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason("STOP").build());
        return ChatResponse.builder().generations(List.of(generation)).build();
    }

    public static ChatResponse toolCallResponse(String toolName, String argumentsJson) {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", toolName, argumentsJson)))
                .build();
        Generation generation = new Generation(message,
                ChatGenerationMetadata.builder().finishReason("TOOL_CALLS").build());
        return ChatResponse.builder().generations(List.of(generation)).build();
    }
}
