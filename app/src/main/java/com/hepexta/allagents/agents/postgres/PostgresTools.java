package com.hepexta.allagents.agents.postgres;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Knowledge-retrieval tools of the {@code postgres-expert} agent: loads the
 * vendored PostgreSQL reference guides by topic. Registered only on the
 * postgres agent's own chat client — the master agent delegates whole
 * questions instead of pulling these guides itself.
 */
@Component
public class PostgresTools {

    public static final String LOAD_REFERENCE_TOOL = "loadReference";

    private final PostgresReferenceLibrary library;

    public PostgresTools(PostgresReferenceLibrary library) {
        this.library = library;
    }

    @Tool(description = """
            Loads a PostgreSQL reference guide by topic and returns its full markdown content.
            Call it before answering questions about schema design, indexing, query optimization,
            partitioning, MVCC/VACUUM, WAL, replication, storage, monitoring, or backup and recovery.""")
    public String loadReference(@ToolParam(description = "Reference topic, one of the names returned in an error message if unknown") String topic) {
        return library.load(topic);
    }
}
