package com.hepexta.allagents.guardrails;

import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.guardrail.Guardrail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class PromptInjectionGuardrail implements Guardrail {

    private final List<String> denyPhrases;

    public PromptInjectionGuardrail(AppProperties properties) {
        this.denyPhrases = properties.guardrails().promptInjection().denyPhrases().stream()
                .map(phrase -> phrase.toLowerCase(Locale.ROOT))
                .toList();
    }

    @Override
    public GuardrailResult checkInput(String input) {
        if (input == null) {
            return GuardrailResult.pass(input);
        }
        String lower = input.toLowerCase(Locale.ROOT);
        for (String phrase : denyPhrases) {
            if (lower.contains(phrase)) {
                return GuardrailResult.block("prompt injection detected");
            }
        }
        return GuardrailResult.pass(input);
    }

    @Override
    public GuardrailResult checkOutput(String output) {
        return GuardrailResult.pass(output);
    }
}
