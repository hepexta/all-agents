package com.hepexta.allagents.adapters.out.memory;

import com.hepexta.allagents.domain.chat.ChatEntry;
import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.domain.chat.ConversationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JdbcConversationRepositoryTest {

    @Autowired
    private JdbcConversationRepository repository;

    @Test
    void createAndFindConversation() {
        Conversation created = repository.create("My conversation", "default");
        Conversation found = repository.find(created.id()).orElseThrow();
        assertEquals("My conversation", found.title());
        assertEquals("default", found.preset());
        assertEquals(created.id(), found.id());
        assertTrue(found.entries().isEmpty());
    }

    @Test
    void appendEntriesAndLoadThemInOrder() {
        Conversation created = repository.create("With entries", null);
        repository.append(created.id(), new ChatEntry("user", "hello", LocalDateTime.now()));
        repository.append(created.id(), new ChatEntry("assistant", "hi!", LocalDateTime.now()));

        Conversation found = repository.find(created.id()).orElseThrow();
        assertEquals(2, found.entries().size());
        assertEquals("user", found.entries().get(0).role());
        assertEquals("assistant", found.entries().get(1).role());
    }

    @Test
    void findAllReturnsAllConversations() {
        repository.create("one", null);
        repository.create("two", null);
        assertTrue(repository.findAll().stream().anyMatch(c -> "one".equals(c.title())));
        assertTrue(repository.findAll().stream().anyMatch(c -> "two".equals(c.title())));
    }

    @Test
    void findMissingReturnsEmpty() {
        assertTrue(repository.find(new ConversationId(UUID.randomUUID().toString())).isEmpty());
    }

    @Test
    void updateTitleChangesTitle() {
        Conversation created = repository.create("Old title", null);
        repository.updateTitle(created.id(), "New title");
        assertEquals("New title", repository.find(created.id()).orElseThrow().title());
    }
}
