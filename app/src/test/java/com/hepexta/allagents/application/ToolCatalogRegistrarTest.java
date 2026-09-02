package com.hepexta.allagents.application;

import com.hepexta.allagents.adapters.out.memory.InMemoryToolCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCatalogRegistrarTest {

    @Test
    void registersToolsInCatalogAndSearchIndex() {
        InMemoryToolCatalog catalog = new InMemoryToolCatalog();
        RegexToolIndex index = new RegexToolIndex();
        ToolCatalogRegistrar registrar = new ToolCatalogRegistrar(catalog, index);
        registrar.afterPropertiesSet();

        assertEquals(1, catalog.all().size());
        assertEquals("getCurrentDate", catalog.all().getFirst().name());
        assertEquals(1, index.size(ToolCatalogRegistrar.TOOL_SEARCH_SESSION));

        var response = index.search(new org.springframework.ai.tool.toolsearch.ToolSearchRequest(
                ToolCatalogRegistrar.TOOL_SEARCH_SESSION, "CurrentDate", 5, null));
        assertTrue(response.toolReferences().stream()
                .anyMatch(ref -> "getCurrentDate".equals(ref.toolName())));
    }
}
