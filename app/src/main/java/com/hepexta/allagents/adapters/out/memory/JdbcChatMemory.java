package com.hepexta.allagents.adapters.out.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Spring AI ChatMemory backed by the embedded H2 database (file-based, survives restarts).
 */
@Component
public class JdbcChatMemory implements ChatMemory {

    private final JdbcTemplate jdbc;

    public JdbcChatMemory(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        messages.forEach(message -> jdbc.update(
                "INSERT INTO chat_memory (conversation_id, role, content) VALUES (?, ?, ?)",
                conversationId, role(message), content(message)));
    }

    @Override
    public List<Message> get(String conversationId) {
        return jdbc.query(
                "SELECT role, content FROM chat_memory WHERE conversation_id = ? ORDER BY id",
                (rs, rowNum) -> toMessage(rs.getString("role"), rs.getString("content")),
                conversationId);
    }

    @Override
    public void clear(String conversationId) {
        jdbc.update("DELETE FROM chat_memory WHERE conversation_id = ?", conversationId);
    }

    private String role(Message message) {
        return message.getMessageType().name().toLowerCase(Locale.ROOT);
    }

    private String content(Message message) {
        return message.getText();
    }

    private Message toMessage(String role, String content) {
        return switch (role) {
            case "user" -> new UserMessage(content);
            default -> new AssistantMessage(content);
        };
    }
}
