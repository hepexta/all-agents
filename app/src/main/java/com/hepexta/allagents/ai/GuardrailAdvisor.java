package com.hepexta.allagents.ai;

import com.hepexta.allagents.domain.guardrail.GuardrailResult;
import com.hepexta.allagents.exception.GuardrailBlockedException;
import com.hepexta.allagents.guardrail.Guardrail;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;

public class GuardrailAdvisor implements BaseAdvisor {

    private final Guardrail guardrail;

    public GuardrailAdvisor(Guardrail guardrail) {
        this.guardrail = guardrail;
    }

    @Override
    public String getName() {
        return "guardrail";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String userText = lastUserText(request);
        if (userText != null) {
            GuardrailResult result = guardrail.checkInput(userText);
            if (!result.allowed()) {
                throw new GuardrailBlockedException(result.reason());
            }
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    private String lastUserText(ChatClientRequest request) {
        List<Message> messages = request.prompt().getInstructions();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getMessageType() == MessageType.USER) {
                return messages.get(i).getText();
            }
        }
        return null;
    }
}
