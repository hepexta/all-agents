package com.hepexta.allagents.application;

import com.hepexta.allagents.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PresetService {

    public static final String DEFAULT_PRESET = "default";

    private final Map<String, AppProperties.Preset> presets;

    public PresetService(AppProperties properties) {
        this.presets = properties.presets();
    }

    public List<PresetInfo> list() {
        return presets.entrySet().stream()
                .map(e -> new PresetInfo(e.getKey(), e.getValue().name()))
                .toList();
    }

    public Optional<AppProperties.Preset> find(String presetId) {
        if (presetId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(presets.get(presetId));
    }

    public record PresetInfo(String id, String name) {
    }
}
