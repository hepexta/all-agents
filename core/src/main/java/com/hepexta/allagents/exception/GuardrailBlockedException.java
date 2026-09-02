package com.hepexta.allagents.exception;

public class GuardrailBlockedException extends RuntimeException {

    public GuardrailBlockedException(String reason) {
        super("blocked by guardrail: " + reason);
    }
}
