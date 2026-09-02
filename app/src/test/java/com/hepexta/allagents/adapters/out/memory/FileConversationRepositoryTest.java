package com.hepexta.allagents.adapters.out.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.chat.ChatEntry;
import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.domain.chat.ConversationId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileConversationRepositoryTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private FileConversationRepository repository() {
        AppProperties properties = new AppProperties(null, null, null, null, null,
                new AppProperties.Persistence("jsonl", tempDir.toString()), null);
        return new FileConversationRepository(properties);
    }

    private Path conversationDir(String id) {
        return tempDir.resolve("conversations").resolve(id);
    }

    @Test
    void createWritesMetaAndFindReturnsConversationWithoutEntries() throws Exception {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("My conversation", "default");

        Path metaPath = conversationDir(created.id().value()).resolve("meta.json");
        assertTrue(Files.exists(metaPath));
        String meta = Files.readString(metaPath);
        assertTrue(meta.contains("\"title\":\"My conversation\""));
        assertTrue(meta.contains("\"preset\":\"default\""));
        assertTrue(meta.contains("\"id\":\"" + created.id().value() + "\""));
        assertTrue(Files.notExists(conversationDir(created.id().value()).resolve("history.jsonl")));

        Conversation found = repository.find(created.id()).orElseThrow();
        assertEquals(created.id(), found.id());
        assertEquals("My conversation", found.title());
        assertEquals("default", found.preset());
        assertEquals(created.createdAt(), found.createdAt());
        assertTrue(found.entries().isEmpty());
    }

    @Test
    void appendEntriesAndLoadThemInOrder() {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("With entries", null);
        LocalDateTime userAt = LocalDateTime.of(2026, 8, 31, 10, 0);
        LocalDateTime assistantAt = LocalDateTime.of(2026, 8, 31, 10, 1);
        repository.append(created.id(), new ChatEntry("user", "hello", userAt));
        repository.append(created.id(), new ChatEntry("assistant", "hi!", assistantAt));

        Conversation found = repository.find(created.id()).orElseThrow();
        assertEquals(2, found.entries().size());
        assertEquals("user", found.entries().get(0).role());
        assertEquals("hello", found.entries().get(0).content());
        assertEquals(userAt, found.entries().get(0).timestamp());
        assertEquals("assistant", found.entries().get(1).role());
        assertEquals("hi!", found.entries().get(1).content());
        assertEquals(assistantAt, found.entries().get(1).timestamp());
    }

    @Test
    void entriesWithQuotesAndNewlinesRoundTrip() {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("Escaping", null);
        String content = "say \"hi\"\nsecond line \\ backslash";
        repository.append(created.id(), new ChatEntry("user", content, LocalDateTime.now()));

        assertEquals(content, repository.find(created.id()).orElseThrow().entries().get(0).content());
    }

    @Test
    void historyIsStoredAsOneJsonLinePerEntry() throws Exception {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("Jsonl shape", null);
        repository.append(created.id(), new ChatEntry("user", "first", LocalDateTime.now()));
        repository.append(created.id(), new ChatEntry("assistant", "second", LocalDateTime.now()));

        List<String> lines = Files.readAllLines(conversationDir(created.id().value()).resolve("history.jsonl"));
        assertEquals(2, lines.size());
        assertEquals("first", mapper.readValue(lines.get(0), ChatEntry.class).content());
        assertEquals("second", mapper.readValue(lines.get(1), ChatEntry.class).content());
    }

    @Test
    void findAllReturnsAllConversationsNewestFirst() throws Exception {
        FileConversationRepository repository = repository();
        Conversation older = repository.create("older", null);
        Conversation newer = repository.create("newer", null);

        ObjectNode oldMeta = (ObjectNode) mapper.readTree(
                Files.readString(conversationDir(older.id().value()).resolve("meta.json")));
        oldMeta.put("createdAt", "2026-01-01T00:00:00");
        Files.writeString(conversationDir(older.id().value()).resolve("meta.json"), oldMeta.toString());

        List<Conversation> all = repository.findAll();
        assertEquals(List.of(newer.id(), older.id()), all.stream().map(Conversation::id).toList());
    }

    @Test
    void findAllIsEmptyWhenDataDirDoesNotExist() {
        assertTrue(repository().findAll().isEmpty());
    }

    @Test
    void findMissingReturnsEmpty() {
        assertTrue(repository().find(new ConversationId(UUID.randomUUID().toString())).isEmpty());
    }

    @Test
    void updateTitleChangesTitleAndKeepsMetadata() {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("Old title", "concise");
        repository.updateTitle(created.id(), "New title");

        Conversation found = repository.find(created.id()).orElseThrow();
        assertEquals("New title", found.title());
        assertEquals("concise", found.preset());
        assertEquals(created.createdAt(), found.createdAt());
    }

    @Test
    void directoryWithoutMetaIsSkippedInFindAll() throws Exception {
        FileConversationRepository repository = repository();
        Files.createDirectories(conversationDir(UUID.randomUUID().toString()));
        repository.create("real", null);

        List<Conversation> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals("real", all.get(0).title());
    }

    @Test
    void corruptedMetaThrowsOnRead() throws Exception {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("will break", null);
        Files.writeString(conversationDir(created.id().value()).resolve("meta.json"), "not-json{{{");

        assertThrows(IllegalStateException.class, () -> repository.find(created.id()));
    }

    @Test
    void tornAndBlankLinesAreSkipped() throws Exception {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("Torn", null);
        repository.append(created.id(), new ChatEntry("user", "good one", LocalDateTime.now()));
        Path history = conversationDir(created.id().value()).resolve("history.jsonl");
        Files.writeString(history, "{\"role\":\"user\",\"content\":\"truncat", java.nio.file.StandardOpenOption.APPEND);
        Files.writeString(history, System.lineSeparator() + System.lineSeparator(), java.nio.file.StandardOpenOption.APPEND);
        repository.append(created.id(), new ChatEntry("assistant", "good two", LocalDateTime.now()));

        List<ChatEntry> entries = repository.find(created.id()).orElseThrow().entries();
        assertEquals(2, entries.size());
        assertEquals("good one", entries.get(0).content());
        assertEquals("good two", entries.get(1).content());
    }

    @Test
    void unreadableHistorySurfacesAsUncheckedIOException() throws Exception {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("Broken history", null);
        Files.createDirectory(conversationDir(created.id().value()).resolve("history.jsonl"));

        assertThrows(java.io.UncheckedIOException.class, () -> repository.find(created.id()));
    }

    @Test
    void appendToUnknownConversationDoesNotThrow() {
        FileConversationRepository repository = repository();
        ConversationId unknown = new ConversationId(UUID.randomUUID().toString());
        assertDoesNotThrow(() -> repository.append(unknown, new ChatEntry("user", "orphan", LocalDateTime.now())));
        assertTrue(repository.find(unknown).isEmpty());
    }

    @Test
    void concurrentAppendsAreSafe() throws Exception {
        FileConversationRepository repository = repository();
        Conversation created = repository.create("Concurrent", null);
        int threads = 4;
        int appendsPerThread = 25;
        try (var executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch start = new CountDownLatch(1);
            for (int t = 0; t < threads; t++) {
                int threadNo = t;
                executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < appendsPerThread; i++) {
                        repository.append(created.id(),
                                new ChatEntry("user", "t" + threadNo + "-" + i, LocalDateTime.now()));
                    }
                    return null;
                });
            }
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(threads * appendsPerThread, repository.find(created.id()).orElseThrow().entries().size());
    }

    @Test
    void traversalIdsAreRejectedAndNothingIsWrittenOutside() throws Exception {
        FileConversationRepository repository = repository();
        ConversationId outside = new ConversationId("../escape");

        assertThrows(IllegalArgumentException.class, () -> repository.find(outside));
        assertThrows(IllegalArgumentException.class,
                () -> repository.append(outside, new ChatEntry("user", "nope", LocalDateTime.now())));
        assertThrows(IllegalArgumentException.class, () -> repository.updateTitle(outside, "nope"));
        assertTrue(Files.notExists(tempDir.resolve("escape")));
    }

    @Test
    void dotDotOnlyIdIsRejected() {
        FileConversationRepository repository = repository();
        ConversationId parent = new ConversationId("..");

        assertThrows(IllegalArgumentException.class, () -> repository.find(parent));
        assertThrows(IllegalArgumentException.class,
                () -> repository.append(parent, new ChatEntry("user", "nope", LocalDateTime.now())));
    }
}
