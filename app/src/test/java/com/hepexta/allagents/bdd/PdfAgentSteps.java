package com.hepexta.allagents.bdd;

import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.exception.AgentExecutionException;
import com.hepexta.allagents.exception.AgentStoppedException;
import com.hepexta.allagents.ports.AgentLifecycleManager;
import com.hepexta.allagents.ports.AgentRuntime;
import com.hepexta.allagents.support.World;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfAgentSteps {

    @Autowired
    private AgentRuntime runtime;

    @Autowired
    private AgentLifecycleManager lifecycleManager;

    @Autowired
    private World world;

    @Given("the pdf agent is stopped")
    public void pdfAgentStopped() {
        lifecycleManager.stop(new AgentId("pdf-extractor"));
    }

    @When("the pdf agent receives instruction {string}")
    public void pdfAgentReceives(String instruction) {
        try {
            world.setLastAgentResult(runtime.executeByName("pdf-extractor",
                    new AgentRequest(instruction, Map.of("pdfBase64", world.getPdfBase64()), null)));
        } catch (RuntimeException e) {
            world.setLastError(e);
        }
    }

    @When("the pdf agent receives instruction {string} without a pdf")
    public void pdfAgentReceivesWithoutPdf(String instruction) {
        try {
            world.setLastAgentResult(runtime.executeByName("pdf-extractor",
                    new AgentRequest(instruction, Map.of(), null)));
        } catch (RuntimeException e) {
            world.setLastError(e);
        }
    }

    @Then("the result contains {string}")
    public void resultContains(String expected) {
        assertTrue(world.getLastAgentResult().content().contains(expected),
                "result does not contain '" + expected + "': " + world.getLastAgentResult().content());
    }

    @Then("the result data contains pages {string}")
    public void resultDataContainsPages(String pages) {
        assertEquals(Integer.parseInt(pages), world.getLastAgentResult().data().get("pages"));
    }

    @Then("the request fails with agent stopped")
    public void failsWithAgentStopped() {
        assertInstanceOf(AgentStoppedException.class, world.getLastError());
    }

    @Then("the request fails with agent execution error")
    public void failsWithExecutionError() {
        assertInstanceOf(AgentExecutionException.class, world.getLastError());
    }
}
