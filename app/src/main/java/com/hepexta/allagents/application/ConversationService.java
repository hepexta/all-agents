package com.hepexta.allagents.application;

import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.domain.chat.ConversationId;
import com.hepexta.allagents.exception.ConversationNotFoundException;
import com.hepexta.allagents.ports.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository repository;

    public ConversationService(ConversationRepository repository) {
        this.repository = repository;
    }

    public Conversation create(String title, String preset) {
        return repository.create(title == null ? "New chat" : title, preset);
    }

    public Conversation get(String id) {
        return repository.find(new ConversationId(id))
                .orElseThrow(() -> new ConversationNotFoundException(new ConversationId(id)));
    }

    public List<Conversation> list() {
        return repository.findAll();
    }
}
