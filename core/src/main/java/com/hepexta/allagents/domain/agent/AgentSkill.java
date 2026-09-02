package com.hepexta.allagents.domain.agent;

import java.util.List;

public record AgentSkill(String id, String name, String description, List<String> examples) {
}
