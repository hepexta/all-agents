package com.hepexta.allagents.adapters.in.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PresetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsConfiguredPresets() throws Exception {
        mockMvc.perform(get("/api/presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='default')]").exists())
                .andExpect(jsonPath("$[?(@.id=='code-review')]").exists())
                .andExpect(jsonPath("$[?(@.id=='concise')]").exists());
    }
}
