package com.hepexta.allagents.agent;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.exception.AgentExecutionException;
import com.hepexta.allagents.exception.AgentStoppedException;
import com.hepexta.allagents.support.StubAgent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractAgentTest {

    @Test
    void agentStartsStopped() {
        StubAgent agent = new StubAgent("test", "result");
        assertEquals(AgentStatus.STOPPED, agent.status());
        assertEquals("test", agent.id().value());
        assertEquals("stub agent", agent.definition().description());
    }

    @Test
    void executeWhileStoppedThrows() {
        StubAgent agent = new StubAgent("test", "result");
        assertThrows(AgentStoppedException.class, () -> agent.execute(new AgentRequest("do")));
    }

    @Test
    void executeAfterStartReturnsResult() {
        StubAgent agent = new StubAgent("test", "result");
        agent.start();
        AgentResult result = agent.execute(new AgentRequest("do"));
        assertEquals("result", result.content());
        assertEquals("test", result.agentId().value());
    }

    @Test
    void stopTransitionsBackToStopped() {
        StubAgent agent = new StubAgent("test", "result");
        agent.start();
        agent.stop();
        assertEquals(AgentStatus.STOPPED, agent.status());
    }

    @Test
    void runtimeFailureIsWrappedInExecutionException() {
        StubAgent agent = new StubAgent("test", "result", new IllegalStateException("boom"));
        agent.start();
        assertThrows(AgentExecutionException.class, () -> agent.execute(new AgentRequest("do")));
    }

    @Test
    void stoppedExceptionFromDoExecuteIsRethrownAsIs() {
        StubAgent agent = new StubAgent("test", "result", new AgentStoppedException(new AgentId("test")));
        agent.start();
        assertThrows(AgentStoppedException.class, () -> agent.execute(new AgentRequest("do")));
    }
}
