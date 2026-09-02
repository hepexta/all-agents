package com.hepexta.allagents.adapters.out.bus;

import com.hepexta.allagents.domain.message.AgentEvent;
import com.hepexta.allagents.domain.message.AgentEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryAgentEventBusTest {

    @Test
    void publishStoresEventInHistory() {
        InMemoryAgentEventBus bus = new InMemoryAgentEventBus();
        AgentEvent event = new AgentEvent(AgentEventType.AGENT_STARTED, "master", null, Instant.now(), null);
        bus.publish(event);
        assertEquals(List.of(event), bus.history());
    }

    @Test
    void subscribersReceivePublishedEvents() {
        InMemoryAgentEventBus bus = new InMemoryAgentEventBus();
        List<AgentEvent> received = new ArrayList<>();
        bus.subscribe(received::add);
        AgentEvent event = new AgentEvent(AgentEventType.AGENT_STOPPED, "pdf-extractor", null, Instant.now(), null);
        bus.publish(event);
        assertEquals(List.of(event), received);
    }

    @Test
    void historyIsBoundedToMaxSize() {
        InMemoryAgentEventBus bus = new InMemoryAgentEventBus();
        for (int i = 0; i < 1001; i++) {
            bus.publish(new AgentEvent(AgentEventType.REQUEST_COMPLETED, "master", String.valueOf(i), Instant.now(), null));
        }
        assertEquals(1000, bus.history().size());
        assertEquals("1000", bus.history().getLast().detail());
    }
}
