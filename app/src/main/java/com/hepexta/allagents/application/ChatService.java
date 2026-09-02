package com.hepexta.allagents.application;

import com.hepexta.allagents.agents.master.MasterAgent;
import com.hepexta.allagents.domain.chat.ChatEntry;
import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.exception.GuardrailBlockedException;
import com.hepexta.allagents.guardrail.Guardrail;
import com.hepexta.allagents.ports.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatService {

    private final MasterAgent masterAgent;
    private final ConversationRepository conversations;
    private final ConversationService conversationService;
    private final Guardrail guardrail;

    public ChatService(MasterAgent masterAgent,
                       ConversationRepository conversations,
                       ConversationService conversationService,
                       Guardrail guardrail) {
        this.masterAgent = masterAgent;
        this.conversations = conversations;
        this.conversationService = conversationService;
        this.guardrail = guardrail;
    }

    public ChatReply chat(String message, String conversationId, String presetId) {
        Conversation conversation = resolveConversation(message, conversationId, presetId);
        conversations.append(conversation.id(), new ChatEntry("user", message, LocalDateTime.now()));
        if (message == null || message.isBlank()) {
            String replyText = "Please enter a message.";
            conversations.append(conversation.id(), new ChatEntry("assistant", replyText, LocalDateTime.now()));
            return new ChatReply(conversation.id().value(), replyText, false);
        }
        try {
            String raw = masterAgent.chat(message, conversation.id().value(), presetId);
            GuardrailResult output = guardrail.checkOutput(raw);
            if (output.allowed()) {
                conversations.append(conversation.id(), new ChatEntry("assistant", output.content(), LocalDateTime.now()));
                return new ChatReply(conversation.id().value(), output.content(), false);
            }
            String refusal = refusal(output.reason());
            conversations.append(conversation.id(), new ChatEntry("assistant", refusal, LocalDateTime.now()));
            return new ChatReply(conversation.id().value(), refusal, true);
        } catch (GuardrailBlockedException e) {
            String refusal = refusal(e.getMessage());
            conversations.append(conversation.id(), new ChatEntry("assistant", refusal, LocalDateTime.now()));
            return new ChatReply(conversation.id().value(), refusal, true);
        }
    }

    private Conversation resolveConversation(String message, String conversationId, String presetId) {
        if (conversationId == null || conversationId.isBlank()) {
            String title = message == null || message.isBlank() ? "New chat" : abbreviate(message, 50);
            return conversationService.create(title, presetId);
        }
        return conversationService.get(conversationId);
    }

    private String refusal(String reason) {
        return "Request blocked: " + reason;
    }

    private String abbreviate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
