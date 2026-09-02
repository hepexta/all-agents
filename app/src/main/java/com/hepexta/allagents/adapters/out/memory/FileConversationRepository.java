package com.hepexta.allagents.adapters.out.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.chat.ChatEntry;
import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.domain.chat.ConversationId;
import com.hepexta.allagents.ports.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Filesystem-backed {@link ConversationRepository} — active when
 * {@code app.persistence.mode=jsonl}.
 *
 * <p>Layout (one folder per conversation):
 * <pre>
 * {dataDir}/conversations/{id}/meta.json     — id, title, preset, createdAt
 * {dataDir}/conversations/{id}/history.jsonl — one JSON ChatEntry per line, append-only
 * </pre>
 *
 * <p>Writes are append-only, so a killed process can at worst leave a torn final
 * line, which is skipped on read. Files are plain JSON and stay inspectable
 * while the app is running.</p>
 */
@Component
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "jsonl")
public class FileConversationRepository implements ConversationRepository {

    private static final Logger log = LoggerFactory.getLogger(FileConversationRepository.class);
    private static final String META_FILE = "meta.json";
    private static final String HISTORY_FILE = "history.jsonl";

    private final Path conversationsDir;
    private final ObjectMapper mapper;

    public FileConversationRepository(AppProperties properties) {
        this.conversationsDir = Path.of(properties.persistence().dataDir())
                .resolve("conversations")
                .toAbsolutePath()
                .normalize();
        // Boot 4.1 autoconfigures Jackson 3 (no ObjectMapper bean), so the
        // adapter owns its Jackson 2 mapper (with jsr310 for LocalDateTime).
        this.mapper = new ObjectMapper().findAndRegisterModules();
    }

    /** JSON shape of meta.json. */
    record Meta(String id, String title, String preset, LocalDateTime createdAt) {
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    @Override
    public Conversation create(String title, String preset) {
        String id = UUID.randomUUID().toString();
        Conversation conversation = new Conversation(new ConversationId(id), title, preset, LocalDateTime.now(), List.of());
        writeMeta(conversation);
        return conversation;
    }

    @Override
    public Optional<Conversation> find(ConversationId id) {
        Path dir = dirFor(id);
        return Files.exists(dir) ? readDir(dir) : Optional.empty();
    }

    @Override
    public List<Conversation> findAll() {
        if (!Files.exists(conversationsDir)) {
            return List.of();
        }
        try (Stream<Path> dirs = uncheck(() -> Files.list(conversationsDir))) {
            return dirs.filter(Files::isDirectory)
                    .map(this::readDir)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(Conversation::createdAt).reversed())
                    .toList();
        }
    }

    @Override
    public synchronized void append(ConversationId conversationId, ChatEntry entry) {
        Path dir = dirFor(conversationId);
        uncheck(() -> Files.createDirectories(dir));
        uncheck(() -> Files.writeString(dir.resolve(HISTORY_FILE),
                mapper.valueToTree(entry) + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND));
    }

    @Override
    public void updateTitle(ConversationId conversationId, String title) {
        Meta meta = readMeta(dirFor(conversationId));
        writeMeta(new Conversation(conversationId, title, meta.preset(), meta.createdAt(), List.of()));
    }

    private Optional<Conversation> readDir(Path dir) {
        if (!Files.exists(dir.resolve(META_FILE))) {
            log.warn("Skipping conversation directory without {}: {}", META_FILE, dir);
            return Optional.empty();
        }
        Meta meta = readMeta(dir);
        return Optional.of(new Conversation(new ConversationId(meta.id()), meta.title(), meta.preset(),
                meta.createdAt(), readEntries(dir)));
    }

    private Meta readMeta(Path dir) {
        try {
            return mapper.readValue(uncheck(() -> Files.readString(dir.resolve(META_FILE))), Meta.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot parse conversation metadata: " + dir.resolve(META_FILE), e);
        }
    }

    private List<ChatEntry> readEntries(Path dir) {
        Path historyPath = dir.resolve(HISTORY_FILE);
        if (!Files.exists(historyPath)) {
            return List.of();
        }
        List<ChatEntry> entries = new ArrayList<>();
        for (String line : uncheck(() -> Files.readAllLines(historyPath))) {
            if (line.isBlank()) {
                continue;
            }
            try {
                entries.add(mapper.readValue(line, ChatEntry.class));
            } catch (JsonProcessingException e) {
                log.warn("Skipping unparseable line in {}: {}", historyPath, line);
            }
        }
        return entries;
    }

    private void writeMeta(Conversation conversation) {
        Path dir = dirFor(conversation.id());
        uncheck(() -> Files.createDirectories(dir));
        uncheck(() -> Files.writeString(dir.resolve(META_FILE),
                mapper.valueToTree(new Meta(conversation.id().value(), conversation.title(),
                        conversation.preset(), conversation.createdAt())).toString()));
    }

    private Path dirFor(ConversationId id) {
        return conversationsDir.resolve(id.value());
    }

    private static <T> T uncheck(IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
