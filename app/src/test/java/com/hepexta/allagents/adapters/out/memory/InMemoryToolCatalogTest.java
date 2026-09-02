package com.hepexta.allagents.adapters.out.memory;

import com.hepexta.allagents.domain.tool.ToolInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryToolCatalogTest {

    private InMemoryToolCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new InMemoryToolCatalog();
        catalog.register(new ToolInfo("getCurrentDate", "Returns the current date and time in ISO-8601 format.", "master"));
        catalog.register(new ToolInfo("delegateToAgent", "Delegates a task to a specialist agent.", "master"));
    }

    @Test
    void allReturnsRegisteredTools() {
        assertEquals(2, catalog.all().size());
    }

    @Test
    void searchMatchesName() {
        assertEquals(1, catalog.search("CurrentDate").size());
        assertEquals("getCurrentDate", catalog.search("CurrentDate").getFirst().name());
    }

    @Test
    void searchMatchesDescription() {
        assertEquals(1, catalog.search("specialist").size());
        assertEquals("delegateToAgent", catalog.search("specialist").getFirst().name());
    }

    @Test
    void searchMatchesOwner() {
        assertEquals(2, catalog.search("master").size());
    }

    @Test
    void blankQueryReturnsAll() {
        assertEquals(2, catalog.search("").size());
        assertEquals(2, catalog.search(null).size());
    }

    @Test
    void unmatchedQueryReturnsEmpty() {
        assertTrue(catalog.search("image-generation").isEmpty());
    }

    @Test
    void multiTokenQueryMatchesAnyToken() {
        assertEquals(1, catalog.search("image CurrentDate").size());
    }
}
