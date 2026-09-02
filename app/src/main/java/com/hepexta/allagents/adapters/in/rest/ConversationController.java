package com.hepexta.allagents.adapters.in.rest;

import com.hepexta.allagents.application.ConversationService;
import com.hepexta.allagents.domain.chat.Conversation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    public record CreateConversationRequest(String title, String preset) {
    }

    @PostMapping
    public Conversation create(@RequestBody(required = false) CreateConversationRequest request) {
        if (request == null) {
            return conversationService.create(null, null);
        }
        return conversationService.create(request.title(), request.preset());
    }

    @GetMapping
    public List<Conversation> list() {
        return conversationService.list();
    }

    @GetMapping("/{id}")
    public Conversation get(@PathVariable String id) {
        return conversationService.get(id);
    }
}
