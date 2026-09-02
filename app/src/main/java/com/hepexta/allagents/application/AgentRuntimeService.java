package com.hepexta.allagents.application;

import com.hepexta.allagents.agent.Agent;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.domain.message.AgentEvent;
import com.hepexta.allagents.domain.message.AgentEventType;
import com.hepexta.allagents.exception.AgentNotFoundException;
import com.hepexta.allagents.exception.GuardrailBlockedException;
import com.hepexta.allagents.guardrail.Guardrail;
import com.hepexta.allagents.ports.AgentEventBus;
import com.hepexta.allagents.ports.AgentRegistry;
import com.hepexta.allagents.ports.AgentRuntime;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AgentRuntimeService implements AgentRuntime {

    private final AgentRegistry registry;
    private final AgentEventBus eventBus;
    private final Guardrail guardrail;

    public AgentRuntimeService(AgentRegistry registry, AgentEventBus eventBus, Guardrail guardrail) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.guardrail = guardrail;
    }

    @Override
    public AgentResult execute(AgentId id, AgentRequest request) {
        Agent agent = registry.find(id)
                .orElseThrow(() -> new AgentNotFoundException(id.value()));
        return execute(agent, request);
    }

    @Override
    public AgentResult executeByName(String name, AgentRequest request) {
        Agent agent = registry.findByName(name)
                .orElseThrow(() -> new AgentNotFoundException(name));
        return execute(agent, request);
    }

    private AgentResult execute(Agent agent, AgentRequest request) {
        GuardrailResult inputCheck = guardrail.checkInput(request.instruction() == null ? "" : request.instruction());
        if (!inputCheck.allowed()) {
            fail(agent, request, inputCheck.reason());
            throw new GuardrailBlockedException(inputCheck.reason());
        }
        try {
            AgentResult result = agent.execute(request);
            GuardrailResult outputCheck = guardrail.checkOutput(result.content());
            if (!outputCheck.allowed()) {
                fail(agent, request, outputCheck.reason());
                throw new GuardrailBlockedException(outputCheck.reason());
            }
            AgentResult guarded = new AgentResult(agent.id(), outputCheck.content(), result.data());
            eventBus.publish(new AgentEvent(
                    AgentEventType.REQUEST_COMPLETED, agent.id().value(), request.correlationId(), Instant.now(), request.correlationId()));
            return guarded;
        } catch (RuntimeException e) {
            fail(agent, request, e.getMessage());
            throw e;
        }
    }

    private void fail(Agent agent, AgentRequest request, String detail) {
        eventBus.publish(new AgentEvent(
                AgentEventType.REQUEST_FAILED, agent.id().value(), detail, Instant.now(), request.correlationId()));
    }
}
