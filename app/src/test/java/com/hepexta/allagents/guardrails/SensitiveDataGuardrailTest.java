package com.hepexta.allagents.guardrails;

import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.support.TestProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataGuardrailTest {

    private final SensitiveDataGuardrail enabledGuardrail =
            new SensitiveDataGuardrail(TestProperties.of(1, 1));

    private final SensitiveDataGuardrail disabledGuardrail =
            new SensitiveDataGuardrail(TestProperties.guardrails(new AppProperties.Guardrails(
                    new AppProperties.Guardrails.PromptInjection(java.util.List.of()),
                    new AppProperties.Guardrails.SensitiveData(false),
                    new AppProperties.Guardrails.Length(100, 100))));

    @Test
    void redactsEmails() {
        GuardrailResult result = enabledGuardrail.checkOutput("Contact john.doe@example.com for help");
        assertTrue(result.allowed());
        assertTrue(result.content().contains("[REDACTED]"));
        assertFalse(result.content().contains("john.doe@example.com"));
        assertEquals("sensitive data redacted", result.reason());
    }

    @Test
    void redactsApiKeysAndCardsAndPhones() {
        GuardrailResult result = enabledGuardrail.checkOutput(
                "key sk-abcdef123456, card 4111-1111-1111-1111, phone +1 555 123 4567");
        assertTrue(result.allowed());
        assertTrue(result.content().contains("[REDACTED]"));
    }

    @Test
    void passesCleanOutput() {
        GuardrailResult result = enabledGuardrail.checkOutput("no sensitive data here");
        assertTrue(result.allowed());
        assertEquals("no sensitive data here", result.content());
    }

    @Test
    void passesNullOutput() {
        assertTrue(enabledGuardrail.checkOutput(null).allowed());
    }

    @Test
    void disabledGuardrailPassesEverything() {
        GuardrailResult result = disabledGuardrail.checkOutput("Contact john.doe@example.com");
        assertTrue(result.allowed());
        assertEquals("Contact john.doe@example.com", result.content());
    }

    @Test
    void inputAlwaysPasses() {
        GuardrailResult result = enabledGuardrail.checkInput("john.doe@example.com");
        assertTrue(result.allowed());
        assertEquals("john.doe@example.com", result.content());
    }
}
