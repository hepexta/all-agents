package com.hepexta.allagents.agents.pdf;

import com.hepexta.allagents.agent.AbstractAgent;
import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.agent.AgentCapability;
import com.hepexta.allagents.domain.agent.AgentDefinition;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.agent.AgentSkill;
import com.hepexta.allagents.exception.AgentExecutionException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PdfExtractionAgent extends AbstractAgent {

    public static final String ID = "pdf-extractor";
    public static final String PAYLOAD_PDF_BASE64 = "pdfBase64";
    public static final String PAYLOAD_PDF_PATH = "pdfPath";

    private final ChatClient chatClient;
    private final PdfTextExtractor extractor;
    private final AppProperties properties;

    public PdfExtractionAgent(ChatClient.Builder builder, PdfTextExtractor extractor, AppProperties properties) {
        super(agentDefinition());
        this.extractor = extractor;
        this.properties = properties;
        this.chatClient = builder
                .defaultSystem(properties.agents().pdfSystemPrompt())
                .build();
    }

    private static AgentDefinition agentDefinition() {
        return new AgentDefinition(
                new AgentId(ID),
                "PDF Extractor",
                "Extracts text and structured data from PDF documents. Returns the requested data in the format asked by the caller.",
                List.of(new AgentCapability("pdf-data-extraction", "Reads PDF files and extracts text or structured data")),
                List.of(new AgentSkill("pdf-text-extraction", "PDF text extraction", "Extract full text from a PDF document",
                                List.of("Give me all text from this contract")),
                        new AgentSkill("pdf-structured-data", "PDF structured data", "Extract structured data (invoices, forms, tables) from a PDF",
                                List.of("Extract invoice number and total from this PDF"))),
                List.of());
    }

    @Override
    protected AgentResult doExecute(AgentRequest request) {
        PdfTextExtractor.ExtractedText extracted = extract(request);
        String prompt = "Extraction instruction: " + request.instruction()
                + "\n\nDocument text:\n" + extracted.text();
        String content = chatClient.prompt().user(prompt).call().content();
        return new AgentResult(id(), content, Map.of("pages", extracted.pages(), "chars", extracted.text().length()));
    }

    private PdfTextExtractor.ExtractedText extract(AgentRequest request) {
        Map<String, Object> payload = request.payload();
        if (payload != null && payload.containsKey(PAYLOAD_PDF_BASE64)) {
            return extractor.fromBase64(String.valueOf(payload.get(PAYLOAD_PDF_BASE64)));
        }
        if (payload != null && payload.containsKey(PAYLOAD_PDF_PATH)) {
            return extractor.fromPath(String.valueOf(payload.get(PAYLOAD_PDF_PATH)));
        }
        throw new AgentExecutionException(id(), new IllegalArgumentException(
                "payload must contain either '" + PAYLOAD_PDF_BASE64 + "' or '" + PAYLOAD_PDF_PATH + "'"));
    }
}
