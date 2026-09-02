package com.hepexta.allagents.guardrails;

import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.support.TestProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LengthGuardrailTest {

    private final LengthGuardrail guardrail = new LengthGuardrail(
            TestProperties.guardrails(new AppProperties.Guardrails(
                    new AppProperties.Guardrails.PromptInjection(java.util.List.of()),
                    new AppProperties.Guardrails.SensitiveData(true),
                    new AppProperties.Guardrails.Length(5, 10))));

    @Test
    void blocksInputLongerThanMax() {
        GuardrailResult result = guardrail.checkInput("123456");
        assertFalse(result.allowed());
        assertEquals("input too long", result.reason());
    }

    @Test
    void passesInputAtMaxLength() {
        assertTrue(guardrail.checkInput("12345").allowed());
    }

    @Test
    void passesNullInput() {
        assertTrue(guardrail.checkInput(null).allowed());
    }

    @Test
    void blocksOutputLongerThanMax() {
        GuardrailResult result = guardrail.checkOutput("12345678901");
        assertFalse(result.allowed());
        assertEquals("output too long", result.reason());
    }

    @Test
    void passesOutputAtMaxLength() {
        GuardrailResult result = guardrail.checkOutput("1234567890");
        assertTrue(result.allowed());
        assertEquals("1234567890", result.content());
    }

    @Test
    void passesNullOutput() {
        assertTrue(guardrail.checkOutput(null).allowed());
    }
}
