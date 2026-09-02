package com.hepexta.allagents.exception;

import com.hepexta.allagents.domain.chat.ConversationId;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(ConversationId id) {
        super("conversation not found: " + id.value());
    }
}
