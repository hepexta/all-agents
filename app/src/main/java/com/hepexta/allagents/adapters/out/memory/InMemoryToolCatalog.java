package com.hepexta.allagents.adapters.out.memory;

import com.hepexta.allagents.domain.tool.ToolInfo;
import com.hepexta.allagents.ports.ToolCatalog;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class InMemoryToolCatalog implements ToolCatalog {

    private final Map<String, ToolInfo> tools = new LinkedHashMap<>();

    @Override
    public void register(ToolInfo tool) {
        tools.put(tool.name(), tool);
    }

    @Override
    public List<ToolInfo> search(String query) {
        if (query == null || query.isBlank()) {
            return all();
        }
        String[] tokens = query.toLowerCase(Locale.ROOT).split("\\s+");
        return tools.values().stream()
                .filter(tool -> matches(tool, tokens))
                .toList();
    }

    @Override
    public List<ToolInfo> all() {
        return List.copyOf(tools.values());
    }

    private boolean matches(ToolInfo tool, String[] tokens) {
        String haystack = (tool.name() + " " + tool.description() + " " + tool.owner()).toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
