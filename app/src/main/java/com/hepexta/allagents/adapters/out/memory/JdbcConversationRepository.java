package com.hepexta.allagents.adapters.out.memory;

import com.hepexta.allagents.domain.chat.ChatEntry;
import com.hepexta.allagents.domain.chat.Conversation;
import com.hepexta.allagents.domain.chat.ConversationId;
import com.hepexta.allagents.ports.ConversationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * H2-backed {@link ConversationRepository} — the default backend
 * (app.persistence.mode=h2); disabled when mode is switched to jsonl.
 */
@Component
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "h2", matchIfMissing = true)
public class JdbcConversationRepository implements ConversationRepository {

    private final JdbcTemplate jdbc;

    public JdbcConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Conversation create(String title, String preset) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO conversations (id, title, preset, created_at) VALUES (?, ?, ?, ?)",
                id, title, preset, now);
        return new Conversation(new ConversationId(id), title, preset, now, List.of());
    }

    @Override
    public Optional<Conversation> find(ConversationId id) {
        List<Conversation> rows = jdbc.query(
                "SELECT id, title, preset, created_at FROM conversations WHERE id = ?",
                (rs, rowNum) -> toConversation(rs),
                id.value());
        return rows.stream().findFirst();
    }

    @Override
    public List<Conversation> findAll() {
        return jdbc.query(
                "SELECT id, title, preset, created_at FROM conversations ORDER BY created_at DESC",
                (rs, rowNum) -> toConversation(rs));
    }

    @Override
    public void append(ConversationId conversationId, ChatEntry entry) {
        jdbc.update("INSERT INTO messages (conversation_id, role, content, created_at) VALUES (?, ?, ?, ?)",
                conversationId.value(), entry.role(), entry.content(), entry.timestamp());
    }

    @Override
    public void updateTitle(ConversationId conversationId, String title) {
        jdbc.update("UPDATE conversations SET title = ? WHERE id = ?", title, conversationId.value());
    }

    private Conversation toConversation(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        List<ChatEntry> entries = jdbc.query(
                "SELECT role, content, created_at FROM messages WHERE conversation_id = ? ORDER BY id",
                (entryRs, rowNum) -> new ChatEntry(
                        entryRs.getString("role"),
                        entryRs.getString("content"),
                        entryRs.getObject("created_at", LocalDateTime.class)),
                id);
        return new Conversation(
                new ConversationId(id),
                rs.getString("title"),
                rs.getString("preset"),
                rs.getObject("created_at", LocalDateTime.class),
                entries);
    }
}
