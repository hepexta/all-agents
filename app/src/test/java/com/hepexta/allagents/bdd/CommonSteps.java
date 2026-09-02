package com.hepexta.allagents.bdd;

import com.hepexta.allagents.agent.Agent;
import com.hepexta.allagents.domain.agent.AgentStatus;
import com.hepexta.allagents.ports.AgentRegistry;
import com.hepexta.allagents.support.MockChatModel;
import com.hepexta.allagents.support.PdfFixtures;
import com.hepexta.allagents.support.World;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class CommonSteps {

    @Autowired
    private ObjectProvider<MockChatModel> mockChatModelProvider;

    @Autowired
    private AgentRegistry registry;

    @Autowired
    private World world;

    @Before
    public void resetScenario() {
        mockChatModelProvider.ifAvailable(MockChatModel::reset);
        registry.all().stream()
                .filter(agent -> agent.status() == AgentStatus.STOPPED)
                .forEach(Agent::start);
        world.resetScenario();
    }

    @Given("the mock LLM will respond with {string}")
    public void mockResponds(String text) {
        mockChatModelProvider.getObject().respondWith(text);
    }

    @Given("a pdf with text {string}")
    public void aPdfWithText(String text) {
        world.setPdfBase64(PdfFixtures.createPdfBase64(text));
    }
}
