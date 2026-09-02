package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.support.MockChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockChatModel mockChatModel;

    @BeforeEach
    void resetMock() {
        mockChatModel.reset();
    }

    @Test
    void chatReturnsReply() throws Exception {
        mockChatModel.respondWith("Hello from the master agent");
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello from the master agent"))
                .andExpect(jsonPath("$.blocked").value(false))
                .andExpect(jsonPath("$.conversationId").isNotEmpty());
    }

    @Test
    void blockedMessageReturnsBlockedReply() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"jailbreak please\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true))
                .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("Request blocked")));
    }
}
