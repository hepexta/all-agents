package com.hepexta.allagents.agents.postgres;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresReferenceLibraryTest {

    private static final String REAL_REFERENCE_DIR = "skills/postgres/references";

    @Test
    void loadsKnownTopicFromBundledReferences() {
        PostgresReferenceLibrary library = new PostgresReferenceLibrary();
        String content = library.load("schema-design");
        assertTrue(content.contains("BIGINT GENERATED ALWAYS AS IDENTITY"));
        assertTrue(content.contains("Schema Design"));
    }

    @Test
    void unknownTopicThrowsAndListsAvailableTopics() {
        PostgresReferenceLibrary library = new PostgresReferenceLibrary();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> library.load("nope"));
        assertTrue(e.getMessage().contains("unknown topic: nope"));
        assertTrue(e.getMessage().contains("schema-design"));
    }

    @Test
    void blankTopicThrows() {
        PostgresReferenceLibrary library = new PostgresReferenceLibrary();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> library.load("   "));
        assertTrue(e.getMessage().contains("topic is required"));
    }

    @Test
    void pathTraversalTopicIsRejected() {
        PostgresReferenceLibrary library = new PostgresReferenceLibrary();
        assertThrows(IllegalArgumentException.class, () -> library.load("../../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> library.load(REAL_REFERENCE_DIR + "/schema-design.md"));
        assertThrows(IllegalArgumentException.class, () -> library.load(".."));
    }

    @Test
    void missingResourceThrowsIllegalState() {
        PostgresReferenceLibrary library =
                new PostgresReferenceLibrary(REAL_REFERENCE_DIR, Map.of("missing", "missing.md"));
        assertThrows(IllegalStateException.class, () -> library.load("missing"));
    }

    @Test
    void availableTopicsIsSortedCommaSeparatedList() {
        PostgresReferenceLibrary library = new PostgresReferenceLibrary();
        String topics = library.availableTopics();
        assertTrue(topics.startsWith("backup-recovery, index-optimization, indexing"));
        assertTrue(topics.endsWith("storage-layout, wal-operations"));
    }

    @Test
    void everyBundledTopicLoads() {
        PostgresReferenceLibrary library = new PostgresReferenceLibrary();
        for (String topic : library.availableTopics().split(", ")) {
            assertEquals(true, !library.load(topic).isBlank(), "topic is blank: " + topic);
        }
    }
}
