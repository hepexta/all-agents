package com.hepexta.allagents.guardrail;

import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeGuardrailTest {

    private static class BlockingGuardrail implements Guardrail {
        @Override
        public GuardrailResult checkInput(String input) {
            return input.contains("inject") ? GuardrailResult.block("blocked by test") : GuardrailResult.pass(input);
        }

        @Override
        public GuardrailResult checkOutput(String output) {
            return GuardrailResult.pass(output);
        }
    }

    private static class RedactingGuardrail implements Guardrail {
        private String lastSeen;

        @Override
        public GuardrailResult checkInput(String input) {
            return GuardrailResult.pass(input);
        }

        @Override
        public GuardrailResult checkOutput(String output) {
            lastSeen = output;
            if (output.contains("secret")) {
                return GuardrailResult.redact(output.replace("secret", "masked"), "redacted");
            }
            return GuardrailResult.pass(output);
        }
    }

    @Test
    void inputPassesWhenAllGuardrailsPass() {
        CompositeGuardrail guardrail = new CompositeGuardrail(List.of(new BlockingGuardrail()));
        GuardrailResult result = guardrail.checkInput("hello");
        assertTrue(result.allowed());
        assertEquals("hello", result.content());
    }

    @Test
    void inputIsBlockedByFirstBlockingGuardrail() {
        CompositeGuardrail guardrail = new CompositeGuardrail(List.of(new BlockingGuardrail(), new RedactingGuardrail()));
        GuardrailResult result = guardrail.checkInput("inject now");
        assertFalse(result.allowed());
        assertEquals("blocked by test", result.reason());
    }

    @Test
    void outputRedactionChainsThroughGuardrails() {
        RedactingGuardrail redactor = new RedactingGuardrail();
        CompositeGuardrail guardrail = new CompositeGuardrail(List.of(redactor, new BlockingGuardrail()));
        GuardrailResult result = guardrail.checkOutput("the secret value");
        assertTrue(result.allowed());
        assertEquals("the masked value", result.content());
        assertEquals("the secret value", redactor.lastSeen);
    }

    @Test
    void outputPassesUnchanged() {
        CompositeGuardrail guardrail = new CompositeGuardrail(List.of(new BlockingGuardrail(), new RedactingGuardrail()));
        GuardrailResult result = guardrail.checkOutput("plain text");
        assertTrue(result.allowed());
        assertEquals("plain text", result.content());
    }

    private static class OutputBlockingGuardrail implements Guardrail {
        @Override
        public GuardrailResult checkInput(String input) {
            return GuardrailResult.pass(input);
        }

        @Override
        public GuardrailResult checkOutput(String output) {
            return output.contains("forbidden") ? GuardrailResult.block("forbidden output") : GuardrailResult.pass(output);
        }
    }

    @Test
    void blockedOutputStopsTheChain() {
        CompositeGuardrail guardrail = new CompositeGuardrail(List.of(new OutputBlockingGuardrail()));
        GuardrailResult result = guardrail.checkOutput("this is forbidden content");
        assertFalse(result.allowed());
        assertEquals("forbidden output", result.reason());
    }
}
