package com.hepexta.allagents.bdd;

import com.hepexta.allagents.application.ChatReply;
import com.hepexta.allagents.application.ChatService;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.ports.AgentLifecycleManager;
import com.hepexta.allagents.support.MockChatModel;
import com.hepexta.allagents.support.World;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MasterAgentSteps {

    @Autowired
    private ChatService chatService;

    @Autowired
    private AgentLifecycleManager lifecycleManager;

    @Autowired
    private ObjectProvider<MockChatModel> mockChatModelProvider;

    @Autowired
    private World world;

    @Given("the mock LLM will call tool {string} with arguments {string} and then respond with {string}")
    public void mockToolCall(String toolName, String arguments, String finalResponse) {
        mockChatModelProvider.getObject().toolCall(toolName, arguments);
        mockChatModelProvider.getObject().respondWith(finalResponse);
    }

    @When("the user sends {string}")
    public void userSends(String message) {
        world.setLastChatReply(chatService.chat(message, null, null));
    }

    @When("the user sends {string} in the current conversation")
    public void userSendsInConversation(String message) {
        ChatReply previous = world.getLastChatReply();
        world.setLastChatReply(chatService.chat(message, previous.conversationId(), null));
    }

    @When("the user sends {string} with preset {string}")
    public void userSendsWithPreset(String message, String preset) {
        world.setLastChatReply(chatService.chat(message, null, preset));
    }

    @Then("the reply is {string}")
    public void replyIs(String expected) {
        assertEquals(expected, world.getLastChatReply().content());
    }

    @Then("the reply contains {string}")
    public void replyContains(String expected) {
        assertTrue(world.getLastChatReply().content().contains(expected),
                "reply does not contain '" + expected + "': " + world.getLastChatReply().content());
    }

    @Then("the reply is not empty")
    public void replyIsNotEmpty() {
        assertFalse(world.getLastChatReply().content().isBlank());
    }

    @Then("the reply is blocked")
    public void replyIsBlocked() {
        assertTrue(world.getLastChatReply().blocked());
    }

    @Then("a new conversation is created")
    public void newConversationCreated() {
        assertNotEquals("", world.getLastChatReply().conversationId());
    }

    @Then("the mock LLM was not called")
    public void mockWasNotCalled() {
        assertTrue(mockChatModelProvider.getObject().prompts().isEmpty());
    }

    @Then("the mock LLM received a tool result containing today's date")
    public void toolResultContainsTodaysDate() {
        String toolResultText = toolResultText();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        assertTrue(toolResultText.contains(today),
                "tool result does not contain today's date (" + today + "): " + toolResultText);
    }

    @Then("the mock LLM received a tool result containing {string}")
    public void toolResultContains(String expected) {
        assertTrue(toolResultText().contains(expected),
                "tool result does not contain '" + expected + "': " + toolResultText());
    }

    @Then("agent {string} is stopped")
    public void agentIsStopped(String name) {
        assertEquals(AgentStatus.STOPPED, lifecycleManager.statusByName(name));
    }

    @Then("the mock LLM received the previous message {string}")
    public void receivedPreviousMessage(String previousMessage) {
        List<Prompt> prompts = mockChatModelProvider.getObject().prompts();
        Prompt lastPrompt = prompts.get(prompts.size() - 1);
        boolean found = lastPrompt.getInstructions().stream()
                .filter(m -> m.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .anyMatch(previousMessage::equals);
        assertTrue(found, "last prompt does not contain previous user message: " + lastPrompt);
    }

    @Then("the prompt contains the preset system prompt {string}")
    public void promptContainsSystemPrompt(String expected) {
        boolean found = mockChatModelProvider.getObject().prompts().stream()
                .flatMap(prompt -> prompt.getInstructions().stream())
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .map(Message::getText)
                .anyMatch(text -> text.contains(expected));
        assertTrue(found, "no prompt contains the preset system prompt: " + expected);
    }

    private String toolResultText() {
        List<Prompt> prompts = mockChatModelProvider.getObject().prompts();
        assertTrue(prompts.size() >= 2, "expected at least two prompts (tool call + final), got " + prompts.size());
        Prompt toolPrompt = prompts.get(prompts.size() - 1);
        return toolPrompt.getInstructions().stream()
                .filter(m -> m.getMessageType() == MessageType.TOOL)
                .map(m -> m instanceof org.springframework.ai.chat.messages.ToolResponseMessage toolResponse
                        ? toolResponse.getResponses().stream()
                                .map(org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse::responseData)
                                .reduce("", (a, b) -> a + b)
                        : m.getText())
                .reduce("", (a, b) -> a + b);
    }
}
