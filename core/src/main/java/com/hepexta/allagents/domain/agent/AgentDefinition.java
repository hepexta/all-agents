package com.hepexta.allagents.domain.agent;

import java.util.List;

public record AgentDefinition(
        AgentId id,
        String name,
        String description,
        List<AgentCapability> capabilities,
        List<AgentSkill> skills,
        List<String> toolNames) {
}
