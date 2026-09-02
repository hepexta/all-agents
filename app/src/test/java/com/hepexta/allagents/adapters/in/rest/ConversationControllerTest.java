package com.hepexta.allagents.adapters.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createWithoutBodyUsesDefaults() throws Exception {
        mockMvc.perform(post("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("New chat"));
    }

    @Test
    void createWithBodyUsesTitleAndPreset() throws Exception {
        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"My chat\",\"preset\":\"code-review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My chat"))
                .andExpect(jsonPath("$.preset").value("code-review"));
    }

    @Test
    void listReturnsConversations() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(0)));
    }

    @Test
    void getConversationById() throws Exception {
        String id = mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"By id\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/conversations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("By id"));
    }

    @Test
    void missingConversationReturns404() throws Exception {
        mockMvc.perform(get("/api/conversations/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("conversation_not_found"));
    }
}
