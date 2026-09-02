package com.hepexta.allagents.application;

import com.hepexta.allagents.domain.chat.ChatEntry;
import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.domain.chat.ConversationId;
import com.hepexta.allagents.exception.ConversationNotFoundException;
import com.hepexta.allagents.ports.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversationServiceTest {

    private static class FakeConversationRepository implements ConversationRepository {
        final Map<String, Conversation> store = new HashMap<>();

        @Override
        public Conversation create(String title, String preset) {
            String id = UUID.randomUUID().toString();
            Conversation conversation = new Conversation(new ConversationId(id), title, preset, LocalDateTime.now(), new ArrayList<>());
            store.put(id, conversation);
            return conversation;
        }

        @Override
        public Optional<Conversation> find(ConversationId id) {
            return Optional.ofNullable(store.get(id.value()));
        }

        @Override
        public List<Conversation> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public void append(ConversationId conversationId, ChatEntry entry) {
            Conversation conversation = store.get(conversationId.value());
            conversation.entries().add(entry);
        }

        @Override
        public void updateTitle(ConversationId conversationId, String title) {
            store.get(conversationId.value());
        }
    }

    private FakeConversationRepository repository;
    private ConversationService service;

    @BeforeEach
    void setUp() {
        repository = new FakeConversationRepository();
        service = new ConversationService(repository);
    }

    @Test
    void createWithNullTitleUsesDefault() {
        Conversation conversation = service.create(null, null);
        assertEquals("New chat", conversation.title());
    }

    @Test
    void createWithTitleAndPreset() {
        Conversation conversation = service.create("My chat", "code-review");
        assertEquals("My chat", conversation.title());
        assertEquals("code-review", conversation.preset());
    }

    @Test
    void getReturnsExistingConversation() {
        Conversation created = service.create("T", null);
        assertEquals(created, service.get(created.id().value()));
    }

    @Test
    void getMissingThrows() {
        assertThrows(ConversationNotFoundException.class, () -> service.get("missing"));
    }

    @Test
    void listReturnsAllConversations() {
        service.create("T1", null);
        service.create("T2", null);
        assertEquals(2, service.list().size());
    }
}
