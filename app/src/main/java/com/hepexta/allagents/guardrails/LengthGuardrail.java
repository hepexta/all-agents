package com.hepexta.allagents.guardrails;

import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.guardrail.Guardrail;
import org.springframework.stereotype.Component;

@Component
public class LengthGuardrail implements Guardrail {

    private final int maxInputChars;
    private final int maxOutputChars;

    public LengthGuardrail(AppProperties properties) {
        this.maxInputChars = properties.guardrails().length().maxInputChars();
        this.maxOutputChars = properties.guardrails().length().maxOutputChars();
    }

    @Override
    public GuardrailResult checkInput(String input) {
        if (input != null && input.length() > maxInputChars) {
            return GuardrailResult.block("input too long");
        }
        return GuardrailResult.pass(input);
    }

    @Override
    public GuardrailResult checkOutput(String output) {
        if (output != null && output.length() > maxOutputChars) {
            return GuardrailResult.block("output too long");
        }
        return GuardrailResult.pass(output);
    }
}
