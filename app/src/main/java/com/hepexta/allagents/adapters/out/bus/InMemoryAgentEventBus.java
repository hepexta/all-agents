package com.hepexta.allagents.adapters.out.bus;

import com.hepexta.allagents.domain.message.AgentEvent;
import com.hepexta.allagents.ports.AgentEventBus;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-memory event bus. The AgentEventBus port is the seam for the future
 * Kafka-backed async mode: swap this bean for a Kafka implementation and
 * agent events flow through Kafka topics (see README, async roadmap).
 */
@Component
public class InMemoryAgentEventBus implements AgentEventBus {

    private static final int MAX_HISTORY = 1000;

    private final Deque<AgentEvent> history = new ConcurrentLinkedDeque<>();
    private final List<Consumer<AgentEvent>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(AgentEvent event) {
        history.addLast(event);
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
        listeners.forEach(listener -> listener.accept(event));
    }

    @Override
    public List<AgentEvent> history() {
        return List.copyOf(history);
    }

    @Override
    public void subscribe(Consumer<AgentEvent> listener) {
        listeners.add(listener);
    }
}
