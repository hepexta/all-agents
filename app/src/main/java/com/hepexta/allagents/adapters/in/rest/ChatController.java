package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.application.ChatReply;
import com.hepexta.allagents.application.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    public record ChatRequest(String message, String conversationId, String preset) {
    }

    @PostMapping
    public ChatReply chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.message(), request.conversationId(), request.preset());
    }
}
