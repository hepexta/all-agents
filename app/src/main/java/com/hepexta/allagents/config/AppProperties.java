package com.hepexta.allagents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Agents agents,
        Map<String, Preset> presets,
        Guardrails guardrails,
        Pdf pdf,
        Memory memory,
        Persistence persistence,
        A2a a2a) {

    public record Agents(String masterSystemPrompt, String pdfSystemPrompt, String postgresSystemPrompt) {
    }

    public record Preset(String name, String systemPrompt, Double temperature) {
    }

    public record Guardrails(
            PromptInjection promptInjection,
            SensitiveData sensitiveData,
            Length length) {

        public record PromptInjection(List<String> denyPhrases) {
        }

        public record SensitiveData(boolean enabled) {
        }

        public record Length(int maxInputChars, int maxOutputChars) {
        }
    }

    public record Pdf(int maxPages, int maxChars, int maxBytes, String allowedDir) {
    }

    public record Memory(int retrieveSize) {
    }

    public record Persistence(String mode, String dataDir) {
    }

    public record A2a(String baseUrl) {
    }
}
