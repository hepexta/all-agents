package com.hepexta.allagents.agents.pdf;

import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.exception.AgentExecutionException;
import com.hepexta.allagents.exception.AgentStoppedException;
import com.hepexta.allagents.support.MockChatModel;
import com.hepexta.allagents.support.PdfFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PdfExtractionAgentTest {

    @Autowired
    private PdfExtractionAgent agent;

    @Autowired
    private MockChatModel mockChatModel;

    @BeforeEach
    void resetMock() {
        mockChatModel.reset();
        agent.start();
    }

    @Test
    void extractsDataFromBase64Pdf() {
        mockChatModel.respondWith("Extracted: Invoice #42");
        AgentResult result = agent.execute(new AgentRequest(
                "Extract invoice data",
                Map.of("pdfBase64", PdfFixtures.createPdfBase64("Invoice #42 Total 100 USD")),
                null));

        assertEquals("Extracted: Invoice #42", result.content());
        assertEquals(1, result.data().get("pages"));
        assertTrue(((Integer) result.data().get("chars")) > 0);
    }

    @Test
    void extractsDataFromPdfPath() {
        mockChatModel.respondWith("Extracted via path");
        byte[] pdf = PdfFixtures.createPdf("Path content");
        java.nio.file.Path tempFile = java.nio.file.Path.of(
                System.getProperty("java.io.tmpdir"), "test-" + System.nanoTime() + ".pdf");
        try {
            java.nio.file.Files.write(tempFile, pdf);
            AgentResult result = agent.execute(new AgentRequest(
                    "Extract", Map.of("pdfPath", tempFile.toString()), null));
            assertEquals("Extracted via path", result.content());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            tempFile.toFile().delete();
        }
    }

    @Test
    void missingPayloadThrowsExecutionException() {
        assertThrows(AgentExecutionException.class,
                () -> agent.execute(new AgentRequest("Extract", Map.of(), null)));
    }

    @Test
    void stoppedAgentRefusesRequests() {
        agent.stop();
        assertThrows(AgentStoppedException.class,
                () -> agent.execute(new AgentRequest("Extract", Map.of(), null)));
    }

    @Test
    void definitionExposesPdfCapabilities() {
        assertEquals("pdf-extractor", agent.id().value());
        assertEquals("PDF Extractor", agent.definition().name());
        assertEquals(1, agent.definition().capabilities().size());
        assertEquals(2, agent.definition().skills().size());
    }
}
