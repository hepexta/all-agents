package com.hepexta.allagents.domain.agent;

import java.util.List;

/**
 * A2A (Agent2Agent) protocol agent card.
 */
public record AgentCard(
        String name,
        String description,
        String url,
        String version,
        Capabilities capabilities,
        List<Skill> skills,
        List<String> defaultInputModes,
        List<String> defaultOutputModes) {

    public record Capabilities(boolean streaming, boolean pushNotifications) {
    }

    public record Skill(String id, String name, String description, List<String> examples) {
    }

    public static AgentCard fromDefinition(AgentDefinition definition, String url) {
        List<Skill> skills = definition.skills().stream()
                .map(s -> new Skill(s.id(), s.name(), s.description(), s.examples()))
                .toList();
        return new AgentCard(
                definition.id().value(),
                definition.description(),
                url,
                "0.0.1",
                new Capabilities(false, false),
                skills,
                List.of("text"),
                List.of("text"));
    }
}
