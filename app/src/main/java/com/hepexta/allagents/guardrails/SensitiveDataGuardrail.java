package com.hepexta.allagents.guardrails;

import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.guardrail.Guardrail;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveDataGuardrail implements Guardrail {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern API_KEY = Pattern.compile("(?i)\\bsk-[a-zA-Z0-9]{8,}\\b");
    private static final Pattern CARD = Pattern.compile("\\b\\d{4}[- ]\\d{4}[- ]\\d{4}[- ]\\d{4}\\b");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s-]{8,}\\d");

    private final boolean enabled;

    public SensitiveDataGuardrail(AppProperties properties) {
        this.enabled = properties.guardrails().sensitiveData().enabled();
    }

    @Override
    public GuardrailResult checkInput(String input) {
        return GuardrailResult.pass(input);
    }

    @Override
    public GuardrailResult checkOutput(String output) {
        if (!enabled || output == null) {
            return GuardrailResult.pass(output);
        }
        String redacted = redact(output);
        if (redacted.equals(output)) {
            return GuardrailResult.pass(output);
        }
        return GuardrailResult.redact(redacted, "sensitive data redacted");
    }

    private String redact(String text) {
        String result = EMAIL.matcher(text).replaceAll("[REDACTED]");
        result = API_KEY.matcher(result).replaceAll("[REDACTED]");
        result = CARD.matcher(result).replaceAll("[REDACTED]");
        result = PHONE.matcher(result).replaceAll("[REDACTED]");
        return result;
    }
}
