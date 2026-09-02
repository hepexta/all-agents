package com.hepexta.allagents.domain.a2a;

import java.util.List;

public record AgentMessage(String messageId, String role, List<TextPart> parts) {

    public record TextPart(String kind, String text) {
    }

    public static AgentMessage of(String messageId, String role, String text) {
        return new AgentMessage(messageId, role, List.of(new TextPart("text", text)));
    }

    public String text() {
        return parts().stream()
                .map(TextPart::text)
                .reduce("", (a, b) -> a + b);
    }
}
