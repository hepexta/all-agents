package com.hepexta.allagents.ports;

import com.hepexta.allagents.domain.message.AgentEvent;

import java.util.List;
import java.util.function.Consumer;

public interface AgentEventBus {

    void publish(AgentEvent event);

    List<AgentEvent> history();

    void subscribe(Consumer<AgentEvent> listener);
}
