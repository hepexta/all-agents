package com.hepexta.allagents.agents.postgres;

import com.hepexta.allagents.agent.AbstractAgent;
import com.hepexta.allagents.config.AppProperties;
import com.hepexta.allagents.domain.agent.AgentCapability;
import com.hepexta.allagents.domain.agent.AgentDefinition;
import com.hepexta.allagents.domain.agent.AgentId;
import com.hepexta.allagents.domain.agent.AgentRequest;
import com.hepexta.allagents.domain.agent.AgentResult;
import com.hepexta.allagents.domain.agent.AgentSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PostgreSQL specialist agent. The master agent delegates PostgreSQL questions
 * to it over A2A; it answers from the vendored reference guides, loading only
 * the topics the question needs via its {@code loadReference} tool.
 */
@Component
public class PostgresExpertAgent extends AbstractAgent {

    public static final String ID = "postgres-expert";

    private final ChatClient chatClient;

    public PostgresExpertAgent(ChatClient.Builder builder, PostgresTools postgresTools, AppProperties properties) {
        super(agentDefinition());
        this.chatClient = builder
                .defaultSystem(properties.agents().postgresSystemPrompt())
                .defaultTools(postgresTools)
                .build();
    }

    private static AgentDefinition agentDefinition() {
        return new AgentDefinition(
                new AgentId(ID),
                "Postgres Expert",
                "Answers PostgreSQL questions: schema design, indexing, query optimization, partitioning, MVCC/VACUUM, replication, backup and recovery, monitoring and operations. Loads topic reference guides and applies them to the question.",
                List.of(new AgentCapability("postgres-schema-design", "Designs PostgreSQL schemas: tables, primary keys, data types, foreign keys"),
                        new AgentCapability("postgres-query-optimization", "Analyzes and optimizes PostgreSQL queries and indexes"),
                        new AgentCapability("postgres-operations", "PostgreSQL operations: MVCC/VACUUM, WAL, replication, monitoring, backup and recovery")),
                List.of(new AgentSkill("postgres-schema-design", "PostgreSQL schema design", "Advise on tables, primary keys, data types and foreign keys",
                                List.of("How should I design primary keys in Postgres?")),
                        new AgentSkill("postgres-query-optimization", "PostgreSQL query optimization", "Optimize slow queries, design and audit indexes",
                                List.of("Why is my query slow? Review my indexes with EXPLAIN analysis")),
                        new AgentSkill("postgres-operations", "PostgreSQL operations", "MVCC/VACUUM tuning, replication, backups and monitoring",
                                List.of("How do I set up backups and replication for Postgres?"))),
                List.of(PostgresTools.LOAD_REFERENCE_TOOL));
    }

    @Override
    protected AgentResult doExecute(AgentRequest request) {
        String content = chatClient.prompt()
                .options(ToolCallingChatOptions.builder())
                .user(request.instruction())
                .call()
                .content();
        return new AgentResult(id(), content);
    }
}
