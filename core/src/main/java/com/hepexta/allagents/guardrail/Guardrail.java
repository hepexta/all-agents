package com.hepexta.allagents.guardrail;

import com.hepexta.allagents.domain.guardrail.GuardrailResult;

public interface Guardrail {

    GuardrailResult checkInput(String input);

    GuardrailResult checkOutput(String output);
}
