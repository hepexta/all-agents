package com.hepexta.allagents.domain.guardrail;

public record GuardrailResult(boolean allowed, String content, String reason) {

    public static GuardrailResult pass(String content) {
        return new GuardrailResult(true, content, null);
    }

    public static GuardrailResult block(String reason) {
        return new GuardrailResult(false, null, reason);
    }

    public static GuardrailResult redact(String content, String reason) {
        return new GuardrailResult(true, content, reason);
    }
}
