package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.application.TaskStore;
import com.hepexta.allagents.domain.a2a.AgentMessage;
import com.hepexta.allagents.domain.a2a.Task;
import com.hepexta.allagents.support.MockChatModel;
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
class A2aControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockChatModel mockChatModel;

    @Autowired
    private TaskStore taskStore;

    @BeforeEach
    void resetMock() {
        mockChatModel.reset();
    }

    @Test
    void agentCardIsServed() throws Exception {
        mockMvc.perform(get("/a2a/agents/master"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("master"))
                .andExpect(jsonPath("$.skills").isNotEmpty())
                .andExpect(jsonPath("$.defaultInputModes[0]").value("text"));
    }

    @Test
    void unknownAgentCardReturns404() throws Exception {
        mockMvc.perform(get("/a2a/agents/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("agent_not_found"));
    }

    @Test
    void messageSendExecutesAgentAndReturnsMessage() throws Exception {
        mockChatModel.respondWith("a2a answer");
        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":"1","method":"message/send","params":{"message":{"role":"user","parts":[{"kind":"text","text":"hello"}]},"contextId":"ctx-1"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.result.role").value("agent"))
                .andExpect(jsonPath("$.result.parts[0].text").value("a2a answer"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void messageStreamFallsBackToSyncResponse() throws Exception {
        mockChatModel.respondWith("streamed answer");
        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":"2","method":"message/stream","params":{"message":{"role":"user","parts":[{"kind":"text","text":"hello"}]}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.parts[0].text").value("streamed answer"));
    }

    @Test
    void failedExecutionReturnsJsonRpcErrorWithoutInternalDetails() throws Exception {
        mockMvc.perform(post("/a2a/agents/pdf-extractor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":"3","method":"message/send","params":{"message":{"role":"user","parts":[{"kind":"text","text":"extract"}]}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32000))
                .andExpect(jsonPath("$.error.message").value("agent execution failed"));
    }

    @Test
    void missingMessageParamReturnsJsonRpcError() throws Exception {
        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"4\",\"method\":\"message/send\",\"params\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32000));
    }

    @Test
    void unknownAgentReturnsJsonRpcError() throws Exception {
        mockMvc.perform(post("/a2a/agents/nope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"5\",\"method\":\"message/send\",\"params\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32001));
    }

    @Test
    void unknownMethodReturnsMethodNotFound() throws Exception {
        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"6\",\"method\":\"tasks/cancel\",\"params\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601));
    }

    @Test
    void tasksGetReturnsStoredTask() throws Exception {
        Task task = Task.working("task-9", "ctx")
                .completed(AgentMessage.of("m-9", "agent", "task result"));
        taskStore.put(task);

        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"7\",\"method\":\"tasks/get\",\"params\":{\"id\":\"task-9\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("task-9"))
                .andExpect(jsonPath("$.result.status.state").value("completed"))
                .andExpect(jsonPath("$.result.message.parts[0].text").value("task result"));
    }

    @Test
    void tasksGetWithUnknownIdReturnsError() throws Exception {
        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"8\",\"method\":\"tasks/get\",\"params\":{\"id\":\"missing\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32002));
    }

    @Test
    void tasksGetWithMissingParamsReturnsError() throws Exception {
        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":\"9\",\"method\":\"tasks/get\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602));
    }

    @Test
    void partsThatAreNotAListReturnJsonRpcError() throws Exception {
        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":"10","method":"message/send","params":{"message":{"role":"user","parts":"not-a-list"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32000));
    }

    @Test
    void nonTextPartsAreIgnoredWhenExtractingText() throws Exception {
        mockChatModel.respondWith("a2a answer");
        mockMvc.perform(post("/a2a/agents/master")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","id":"11","method":"message/send","params":{"message":{"role":"user","parts":[{"kind":"file","text":"ignore me"},{"kind":"text","text":"hello"}]}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.parts[0].text").value("a2a answer"));
    }
}
