package com.hepexta.allagents.adapters.out.a2a;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpA2aClientTest {

    @Test
    void sendMessagePostsJsonRpcAndParsesAgentMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8080/a2a/agents/pdf-extractor"))
                .andRespond(withSuccess("""
                        {"jsonrpc":"2.0","id":"1","result":{"messageId":"m-1","role":"agent","parts":[{"kind":"text","text":"hello from http"}]},"error":null}
                        """, MediaType.APPLICATION_JSON));

        HttpA2aClient client = new HttpA2aClient(builder, "http://localhost:8080", new ObjectMapper());
        var message = client.sendMessage("pdf-extractor", "extract", "ctx-1");
        assertEquals("hello from http", message.text());
        assertEquals("agent", message.role());
        server.verify();
    }

    @Test
    void nullContextIdIsSentAsEmptyString() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8080/a2a/agents/master"))
                .andRespond(withSuccess("""
                        {"jsonrpc":"2.0","id":"1","result":{"messageId":"m-1","role":"agent","parts":[{"kind":"text","text":"ok"}]},"error":null}
                        """, MediaType.APPLICATION_JSON));

        HttpA2aClient client = new HttpA2aClient(builder, "http://localhost:8080", new ObjectMapper());
        assertEquals("ok", client.sendMessage("master", "hi", null).text());
    }

    @Test
    void emptyResultThrows() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8080/a2a/agents/master"))
                .andRespond(withSuccess("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":null}", MediaType.APPLICATION_JSON));

        HttpA2aClient client = new HttpA2aClient(builder, "http://localhost:8080", new ObjectMapper());
        assertThrows(IllegalStateException.class, () -> client.sendMessage("master", "hi", null));
    }
}
