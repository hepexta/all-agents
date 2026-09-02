package com.hepexta.allagents.support;

import com.hepexta.allagents.config.AppProperties;

import java.util.List;
import java.util.Map;

public final class TestProperties {

    private TestProperties() {
    }

    public static AppProperties of(int pdfMaxPages, int pdfMaxChars) {
        return new AppProperties(
                new AppProperties.Agents("master prompt", "pdf prompt"),
                Map.of(),
                new AppProperties.Guardrails(
                        new AppProperties.Guardrails.PromptInjection(List.of("ignore previous instructions")),
                        new AppProperties.Guardrails.SensitiveData(true),
                        new AppProperties.Guardrails.Length(1000, 1000)),
                new AppProperties.Pdf(pdfMaxPages, pdfMaxChars),
                new AppProperties.Memory(20),
                new AppProperties.Persistence("h2", "./data"),
                new AppProperties.A2a("http://localhost:8080"));
    }

    public static AppProperties presets(Map<String, AppProperties.Preset> presets) {
        AppProperties base = of(10, 1000);
        return new AppProperties(base.agents(), presets, base.guardrails(), base.pdf(), base.memory(), base.persistence(), base.a2a());
    }

    public static AppProperties guardrails(AppProperties.Guardrails guardrails) {
        AppProperties base = of(10, 1000);
        return new AppProperties(base.agents(), base.presets(), guardrails, base.pdf(), base.memory(), base.persistence(), base.a2a());
    }
}
