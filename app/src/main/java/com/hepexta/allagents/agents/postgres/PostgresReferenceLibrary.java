package com.hepexta.allagents.agents.postgres;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads the vendored PostgreSQL reference guides (MIT-licensed, from the PlanetScale
 * database-skills repository, PlanetScale-specific topics trimmed).
 *
 * <p>Topics are resolved through a fixed allowlist — topic names are never used as
 * file paths, so no caller-supplied path can reach outside
 * {@code skills/postgres/references/}.</p>
 */
@Component
public class PostgresReferenceLibrary {

    private static final String DEFAULT_BASE_DIR = "skills/postgres/references";

    private final String baseDir;
    private final Map<String, String> topics;

    public PostgresReferenceLibrary() {
        this(DEFAULT_BASE_DIR, defaultTopics());
    }

    PostgresReferenceLibrary(String baseDir, Map<String, String> topics) {
        this.baseDir = baseDir;
        this.topics = topics;
    }

    private static Map<String, String> defaultTopics() {
        return Map.ofEntries(
                Map.entry("schema-design", "schema-design.md"),
                Map.entry("indexing", "indexing.md"),
                Map.entry("index-optimization", "index-optimization.md"),
                Map.entry("partitioning", "partitioning.md"),
                Map.entry("query-patterns", "query-patterns.md"),
                Map.entry("optimization-checklist", "optimization-checklist.md"),
                Map.entry("mvcc-vacuum", "mvcc-vacuum.md"),
                Map.entry("mvcc-transactions", "mvcc-transactions.md"),
                Map.entry("wal-operations", "wal-operations.md"),
                Map.entry("replication", "replication.md"),
                Map.entry("storage-layout", "storage-layout.md"),
                Map.entry("process-architecture", "process-architecture.md"),
                Map.entry("memory-management-ops", "memory-management-ops.md"),
                Map.entry("monitoring", "monitoring.md"),
                Map.entry("backup-recovery", "backup-recovery.md"));
    }

    /** Returns the markdown reference for the given topic name. */
    public String load(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic is required, available topics: " + availableTopics());
        }
        String file = topics.get(topic);
        if (file == null) {
            throw new IllegalArgumentException("unknown topic: " + topic + ", available topics: " + availableTopics());
        }
        try {
            return new ClassPathResource(baseDir + "/" + file).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot load postgres reference: " + topic, e);
        }
    }

    /** Sorted, comma-separated list of loadable topics (for prompts and error messages). */
    public String availableTopics() {
        return topics.keySet().stream().sorted().collect(Collectors.joining(", "));
    }
}
