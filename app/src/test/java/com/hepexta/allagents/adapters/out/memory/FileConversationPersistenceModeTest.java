package com.hepexta.allagents.adapters.out.memory;

import com.hepexta.allagents.domain.chat.ChatEntry;
import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.ports.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the app.persistence.mode switch: with mode=jsonl the
 * FileConversationRepository is the active {@link ConversationRepository} and the
 * JDBC adapter is not present at all. (The h2 default is covered by
 * {@link JdbcConversationRepositoryTest} and the BDD features.)
 */
@SpringBootTest(properties = {
        "app.persistence.mode=jsonl",
        "app.persistence.data-dir=${java.io.tmpdir}/all-agents-jsonl-test"})
class FileConversationPersistenceModeTest {

    @Autowired
    private ConversationRepository repository;

    @Autowired
    private ApplicationContext context;

    @Test
    void jsonlModeWiresFileRepositoryOnly() {
        assertInstanceOf(FileConversationRepository.class, repository);
        assertTrue(context.getBeansOfType(JdbcConversationRepository.class).isEmpty());
    }

    @Test
    void conversationsPersistThroughThePort() {
        Conversation created = repository.create("jsonl chat", "default");
        repository.append(created.id(), new ChatEntry("user", "hello files", LocalDateTime.now()));
        repository.append(created.id(), new ChatEntry("assistant", "hi from disk", LocalDateTime.now()));

        Conversation found = repository.find(created.id()).orElseThrow();
        assertEquals("jsonl chat", found.title());
        assertEquals(2, found.entries().size());
        assertEquals("hello files", found.entries().get(0).content());
        assertEquals("hi from disk", found.entries().get(1).content());
        assertTrue(repository.findAll().stream().anyMatch(c -> c.id().equals(created.id())));
    }
}
