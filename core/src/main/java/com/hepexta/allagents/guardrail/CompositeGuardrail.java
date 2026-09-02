package com.hepexta.allagents.guardrail;

import com.hepexta.allagents.domain.guardrail.GuardrailResult;

import java.util.List;

public class CompositeGuardrail implements Guardrail {

    private final List<Guardrail> guardrails;

    public CompositeGuardrail(List<Guardrail> guardrails) {
        this.guardrails = List.copyOf(guardrails);
    }

    @Override
    public GuardrailResult checkInput(String input) {
        for (Guardrail guardrail : guardrails) {
            GuardrailResult result = guardrail.checkInput(input);
            if (!result.allowed()) {
                return result;
            }
        }
        return GuardrailResult.pass(input);
    }

    @Override
    public GuardrailResult checkOutput(String output) {
        String content = output;
        for (Guardrail guardrail : guardrails) {
            GuardrailResult result = guardrail.checkOutput(content);
            if (!result.allowed()) {
                return result;
            }
            content = result.content();
        }
        return GuardrailResult.pass(content);
    }
}
