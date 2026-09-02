package com.hepexta.allagents.domain.chat;

import com.fasterxml.jackson.annotation.JsonValue;

public record ConversationId(@JsonValue String value) {

    public ConversationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("conversation id must not be blank");
        }
    }
}
