package com.hepexta.allagents.application;

import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.support.TestProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetServiceTest {

    private final PresetService service = new PresetService(TestProperties.presets(Map.of(
            "default", new AppProperties.Preset("Default", null, 0.7),
            "code-review", new AppProperties.Preset("Code Review", "be strict", 0.2))));

    @Test
    void listsAllPresets() {
        var presets = service.list();
        assertEquals(2, presets.size());
        assertTrue(presets.stream().anyMatch(p -> "default".equals(p.id()) && "Default".equals(p.name())));
        assertTrue(presets.stream().anyMatch(p -> "code-review".equals(p.id()) && "Code Review".equals(p.name())));
    }

    @Test
    void findReturnsKnownPreset() {
        assertTrue(service.find("code-review").isPresent());
        assertEquals("be strict", service.find("code-review").orElseThrow().systemPrompt());
        assertEquals(0.2, service.find("code-review").orElseThrow().temperature());
    }

    @Test
    void findReturnsEmptyForNullAndUnknown() {
        assertTrue(service.find(null).isEmpty());
        assertTrue(service.find("unknown").isEmpty());
    }
}
