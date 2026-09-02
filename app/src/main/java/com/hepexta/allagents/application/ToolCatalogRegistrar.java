package com.hepexta.allagents.application;

import com.hepexta.allagents.domain.tool.ToolInfo;
import com.hepexta.allagents.ports.ToolCatalog;
import com.hepexta.allagents.tools.CurrentDateTool;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class ToolCatalogRegistrar implements InitializingBean {

    public static final String TOOL_SEARCH_SESSION = "default";

    private final ToolCatalog catalog;
    private final ToolIndex toolIndex;

    public ToolCatalogRegistrar(ToolCatalog catalog, ToolIndex toolIndex) {
        this.catalog = catalog;
        this.toolIndex = toolIndex;
    }

    @Override
    public void afterPropertiesSet() {
        register(new ToolInfo(CurrentDateTool.NAME, "Returns the current date and time in ISO-8601 format.", "master"));
    }

    private void register(ToolInfo tool) {
        catalog.register(tool);
        toolIndex.indexTool(TOOL_SEARCH_SESSION,
                ToolReference.builder().toolName(tool.name()).summary(tool.description()).build());
    }
}
