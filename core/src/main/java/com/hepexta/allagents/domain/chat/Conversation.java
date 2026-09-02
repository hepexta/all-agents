package com.hepexta.allagents.domain.chat;

import java.time.LocalDateTime;
import java.util.List;

public record Conversation(
        ConversationId id,
        String title,
        String preset,
        LocalDateTime createdAt,
        List<ChatEntry> entries) {
}
