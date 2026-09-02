package com.hepexta.allagents.ports;

import com.hepexta.allagents.domain.tool.ToolInfo;

import java.util.List;

public interface ToolCatalog {

    void register(ToolInfo tool);

    List<ToolInfo> search(String query);

    List<ToolInfo> all();
}
