package com.hepexta.allagents.ports;

import com.hepexta.allagents.domain.chat.ChatEntry;
import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.domain.chat.ConversationId;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    Conversation create(String title, String preset);

    Optional<Conversation> find(ConversationId id);

    List<Conversation> findAll();

    void append(ConversationId conversationId, ChatEntry entry);

    void updateTitle(ConversationId conversationId, String title);
}
