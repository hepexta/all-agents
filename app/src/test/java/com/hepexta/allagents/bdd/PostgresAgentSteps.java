package com.hepexta.allagents.bdd;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.ports.AgentLifecycleManager;
import com.hepexta.allagents.ports.AgentRuntime;
import com.hepexta.allagents.support.World;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class PostgresAgentSteps {

    @Autowired
    private AgentRuntime runtime;

    @Autowired
    private AgentLifecycleManager lifecycleManager;

    @Autowired
    private World world;

    @Given("the postgres agent is stopped")
    public void postgresAgentStopped() {
        lifecycleManager.stop(new AgentId("postgres-expert"));
    }

    @When("the postgres agent receives instruction {string}")
    public void postgresAgentReceives(String instruction) {
        try {
            world.setLastAgentResult(runtime.executeByName("postgres-expert",
                    new AgentRequest(instruction, Map.of(), null)));
        } catch (RuntimeException e) {
            world.setLastError(e);
        }
    }
}
