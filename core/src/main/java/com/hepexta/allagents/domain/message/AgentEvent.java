package com.hepexta.allagents.domain.message;

import java.time.Instant;

public record AgentEvent(
        AgentEventType type,
        String agentName,
        String detail,
        Instant timestamp,
        String correlationId) {
}
