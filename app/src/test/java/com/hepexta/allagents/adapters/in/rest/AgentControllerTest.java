package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.support.MockChatModel;
import com.hepexta.allagents.support.PdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockChatModel mockChatModel;

    @BeforeEach
    void resetMock() {
        mockChatModel.reset();
    }

    @Test
    void agentsListsCardsWithSkillsAndCapabilities() throws Exception {
        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='master')]").exists())
                .andExpect(jsonPath("$[?(@.name=='pdf-extractor')]").exists())
                .andExpect(jsonPath("$[0].capabilities").isNotEmpty())
                .andExpect(jsonPath("$[0].skills").isNotEmpty())
                .andExpect(jsonPath("$[0].url").isNotEmpty());
    }

    @Test
    void startStopAndStatus() throws Exception {
        mockMvc.perform(post("/api/agents/pdf-extractor/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STOPPED"));
        mockMvc.perform(get("/api/agents/pdf-extractor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STOPPED"));
        mockMvc.perform(post("/api/agents/pdf-extractor/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTED"));
    }

    @Test
    void unknownAgentReturns404() throws Exception {
        mockMvc.perform(get("/api/agents/nope/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("agent_not_found"));
        mockMvc.perform(post("/api/agents/nope/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    void executeDelegatesToAgent() throws Exception {
        mockChatModel.respondWith("Extracted: invoice data");
        mockMvc.perform(post("/api/agents/pdf-extractor/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"Extract\",\"payload\":{\"pdfBase64\":\"" + PdfFixtures.createPdfBase64("Data") + "\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Extracted: invoice data"))
                .andExpect(jsonPath("$.agentId").value("pdf-extractor"))
                .andExpect(jsonPath("$.data.pages").value(1));
    }

    @Test
    void executeOnStoppedAgentReturns409() throws Exception {
        mockMvc.perform(post("/api/agents/pdf-extractor/stop"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/agents/pdf-extractor/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"Extract\",\"payload\":{}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("agent_stopped"));
        mockMvc.perform(post("/api/agents/pdf-extractor/start"));
    }

    @Test
    void toolsListsRegisteredTools() throws Exception {
        mockMvc.perform(get("/api/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='getCurrentDate')]").exists());
    }
}
