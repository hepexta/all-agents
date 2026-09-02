package com.hepexta.allagents.support;

import com.hepexta.allagents.application.ChatReply;
import com.hepexta.allagents.domain.agent.AgentResult;
import org.springframework.stereotype.Component;

/**
 * Shared scenario state for BDD steps (Spring context is shared across scenarios).
 */
@Component
public class World {

    private ChatReply lastChatReply;
    private RuntimeException lastError;
    private AgentResult lastAgentResult;
    private String pdfBase64;

    public ChatReply getLastChatReply() {
        return lastChatReply;
    }

    public void setLastChatReply(ChatReply lastChatReply) {
        this.lastChatReply = lastChatReply;
    }

    public RuntimeException getLastError() {
        return lastError;
    }

    public void setLastError(RuntimeException lastError) {
        this.lastError = lastError;
    }

    public AgentResult getLastAgentResult() {
        return lastAgentResult;
    }

    public void setLastAgentResult(AgentResult lastAgentResult) {
        this.lastAgentResult = lastAgentResult;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }

    public void resetScenario() {
        lastChatReply = null;
        lastError = null;
        lastAgentResult = null;
        pdfBase64 = null;
    }
}
