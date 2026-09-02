# Conversation Persistence — Diagnosis & Proposals

Status: proposal · Date: 2026-08-31 · Applies to: `app` module (H2 chat history)

> **Update 2026-08-31 — Option C implemented.** `app.persistence.mode=jsonl` now
> activates `FileConversationRepository` (see section 5, Option C) behind the
> existing `ConversationRepository` port; default stays `h2`. Files:
> `app/.../adapters/out/memory/FileConversationRepository.java`,
> tests: `FileConversationRepositoryTest`, `FileConversationPersistenceModeTest`.
> Switch: `app.persistence.mode: h2|jsonl` + `app.persistence.data-dir` in `application.yml`.

## 1. Symptom

The UI lists conversations correctly, but when a conversation is loaded, only the
**first sentence** (the very first user message) is shown — the rest of the chat
history is missing.

## 2. What I found (evidence)

Live inspection of the running backend (`GET /api/conversations`) against the H2
file DB (`./data/all-agents.mv.db`, 77 KB) shows the pattern clearly:

| Conversation | Created | Entries persisted |
|---|---|---|
| `88b7accb…` | 21:04 | 1 — the first user message only |
| `d18590ee…` | 21:08 | 1 — the first user message only |
| `bdcf786d…` | 21:09 | 1 — the first user message only |
| `3e112128…` | 21:12 | 1 — the first user message only |
| `1abbb84e…` | 21:17 | 6 — user + assistant for every turn ✅ |

So persistence **does work** — the newest conversation has full history. The older
conversations are not corrupted reads; they genuinely contain a single row in
`messages`: the first user message. The assistant reply was never written.

## 3. Root cause

### 3.1 Non-atomic turn persistence (primary cause)

`ChatService.chat()` (`app/.../application/ChatService.java:32-55`) persists a turn
in two autocommitted steps with a **long synchronous LLM call in between**:

1. `conversations.append(user message)` — committed immediately (JdbcTemplate, autocommit),
2. `masterAgent.chat(...)` — synchronous model call, can take minutes ([1m] model, 300 s UI timeout),
3. `conversations.append(assistant reply)` — only on the success/guardrail paths.

The assistant entry is written **only after** the call returns. Every failure mode
between steps 1 and 3 leaves the conversation with exactly one entry — the first
sentence:

- model endpoint error / timeout (any exception other than `GuardrailBlockedException`
  propagates out of `ChatService`; `GlobalExceptionHandler` returns 500/409 but nothing is persisted),
- backend process killed mid-call (Ctrl+C, `Taskkill`, IDE stop) — the known
  restart quirk in this environment (see `project_backend_runtime` note: the javapath
  shim process tree must be killed before restarts, which makes mid-call kills routine),
- JVM crash while a call is in flight.

The DB itself is consistent (each insert is atomic in MVStore) — this is a
**business-flow gap**, not data corruption.

### 3.2 Dual persistence paths drift

Two independent stores exist for the same conversation:

| Store | Table | Writer | Reader |
|---|---|---|---|
| UI history | `messages` | `ChatService` (append) | `GET /api/conversations/{id}` → UI |
| LLM context memory | `chat_memory` | Spring AI `MessageChatMemoryAdvisor` → `JdbcChatMemory.add` | advisor `before()` → injected into prompt |

They can diverge:

- A guardrail-blocked turn is appended to `messages` (refusal text) but never reaches
  `chat_memory` (the guardrail throws before the model call, so the advisor's `after`
  never runs).
- A tool-call-only assistant message has **null text** (`AbstractMessage.getText()`
  in Spring AI 2.0.1 returns `textContent` without throwing) → `JdbcChatMemory.content()`
  inserts `NULL` into `chat_memory.content`, and `JdbcChatMemory` has no timestamp
  column either.
- Two writers for one conversation = permanent sync burden.

### 3.3 CWD-relative DB path trap

`application.yml` uses `jdbc:h2:file:./data/all-agents` — relative to the **process
working directory**. `scripts/start.sh` does `cd` to the project root first, but
running the jar from anywhere else (`java -jar app/target/…jar` from another
directory) silently creates a **fresh empty database** — "history disappeared"
variant of the same symptom.

### 3.4 Embedded H2 file lock

The embedded (in-process) H2 holds an OS-level file lock while the app runs. Any
second connection — H2 Shell, H2 console, a backup script — fails with
`MVStoreException: The file is locked` (verified today; see `data/all-agents.trace.db`).
Backup/repair/inspection requires stopping the app, unless the URL is switched to
`AUTO_SERVER=TRUE`.

## 4. The fix for H2 (Option A — recommended)

Keep H2; repair the flow. Zero new dependencies, smallest diff, fits the existing
hexagonal ports (`ConversationRepository` stays the seam).

### 4.1 Always persist the turn outcome

Change `ChatService.chat()` so **every** turn ends with an assistant entry —
success, refusal, or error. The catch-all is the fix:

```java
public ChatReply chat(String message, String conversationId, String presetId) {
    Conversation conversation = resolveConversation(message, conversationId, presetId);
    conversations.append(conversation.id(), new ChatEntry("user", message, now()));
    if (message == null || message.isBlank()) { … /* unchanged */ }
    try {
        String raw = masterAgent.chat(message, conversation.id().value(), presetId);
        GuardrailResult output = guardrail.checkOutput(raw);
        if (output.allowed()) { … } else { …refusal… }
    } catch (GuardrailBlockedException e) {
        …refusal…                                  // unchanged
    } catch (Exception e) {                        // NEW — no more dangling turns
        String error = "Master agent failed: " + rootMessage(e);   // don't leak credentials
        conversations.append(conversation.id(), new ChatEntry("assistant", error, now()));
        log.error("chat failed for conversation {}", conversation.id(), e);
        return new ChatReply(conversation.id().value(), error, true);
    }
}
```

This does not make the turn transactional (you should NOT hold a DB transaction
across a minutes-long model call — it would pin the connection). It guarantees the
invariant the UI depends on: *an appended user entry is always followed by an
assistant entry*, so loading a conversation never shows "just the first sentence".

### 4.2 Unify the two stores (single source of truth)

`messages` should be the only store. Options, in order of preference:

1. **Advisor reads from `messages`** — reimplement `JdbcChatMemory.get()` as a
   `SELECT role, content FROM messages WHERE conversation_id = ? ORDER BY id`
   (map `user`/`assistant` roles back to Spring AI messages), and drop the
   `chat_memory` table and `add()`/`clear()` writes. `ChatService` remains the
   single writer. Guardrail-blocked turns then appear in the LLM's memory too,
   which is actually desirable (the model should know its previous response was
   blocked).
2. Or **`JdbcChatMemory` delegates to `ConversationRepository`** (port call instead
   of SQL) — same result, cleaner layering.
3. Least preferred: keep both tables and synchronize writes — reintroduces the
   divergence problem.

Either way, the `chat_memory` table can be dropped from `schema.sql` (plus a
one-time `DROP TABLE chat_memory;` for existing DBs — its data is duplicated in
`messages`).

### 4.3 H2 hardening

```yaml
spring:
  datasource:
    # absolute, CWD-independent path
    url: jdbc:h2:file:${ALL_AGENTS_DATA_DIR:./data}/all-agents;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE
```

- `AUTO_SERVER=TRUE` — allows a second connection (H2 Shell / console / backup
  script) while the app is running. Removes the "file is locked" wall.
- Absolute path or env-overridable `ALL_AGENTS_DATA_DIR` — kills the CWD trap (3.3).
- Add an index: `CREATE INDEX IF NOT EXISTS idx_messages_conv ON messages(conversation_id);`
- Backup story: `BACKUP TO '…zip'` (works with `AUTO_SERVER`), or copy
  `all-agents.mv.db` during a clean shutdown; run `COMPACT` after restores.

### 4.4 Cleanup of existing dangling conversations

One-off repair against the current DB (app stopped, or via H2 Shell with
`AUTO_SERVER`):

```sql
-- delete conversations that never got past the first message
DELETE FROM conversations WHERE id IN (
    SELECT conversation_id FROM messages GROUP BY conversation_id HAVING COUNT(*) = 1
);
DELETE FROM chat_memory WHERE conversation_id NOT IN (SELECT id FROM conversations);
```

### 4.5 Tests required (JaCoCo 100% gate)

- BDD feature (`features/mock`): "chat that fails mid-call persists an assistant
  error entry" — extend the scripted `MockChatModel` queue with an error script
  (`MockChatModel` records every `Prompt`; add a failure mode), then assert
  `GET /api/conversations/{id}` has 2 entries.
- Unit tests for the new `catch (Exception)` branch and for `JdbcChatMemory`
  reading from `messages` (if 4.2 option 1/2 is taken).
- Keep the `mock` profile unaffected: the persistence adapter must remain a plain
  bean so tests can swap it.

**Effort: ~0.5–1 day.** Risk: low. Nothing outside `app` changes; ports unchanged.

## 5. Alternatives for filesystem persistence

The `ConversationRepository` port (hexagonal out-port) is the seam — any of these
replaces `JdbcConversationRepository` with a new adapter, one class + tests.

### Candidate landscape (shortlist → top 3 below)

| Candidate | Kind | Verdict |
|---|---|---|
| **H2 (repaired)** | embedded SQL DB | keep — Option A |
| **SQLite** | embedded SQL DB, single file | strong — Option B |
| **JSONL append-only files** | plain files | strong — Option C |
| Nitrite | pure-Java NoSQL (Bson) | viable; weaker ecosystem than SQLite |
| MapDB | embedded KV/collections | viable; less readable files, niche |
| RocksDB / Xodus | embedded KV (LSM) | overkill for chat history |
| PostgreSQL | server DB | not "filesystem"; out of scope (future async mode) |
| Mongo / Redis | server stores | out of scope, same reason |

### Option B — SQLite (single-file SQL, WAL)

- **What**: swap the H2 dependency for `org.xerial:sqlite-jdbc` (or
  `spring-boot-starter-data-jdbc` with the SQLite dialect) behind the same
  `JdbcTemplate` code. One file (`data/all-agents.sqlite`), same `schema.sql`
  (minor dialect tweaks: `TEXT`/`INTEGER PRIMARY KEY` instead of `VARCHAR`/`BIGINT
  AUTO_INCREMENT`).
- **Why it fits Spring**: identical programming model to today (`JdbcTemplate`,
  `DataSource`, `spring.sql.init`); the change is almost entirely config +
  dialect SQL.
- **Pros**: single portable file; **no exclusive file lock** — WAL allows a
  second reader, backup scripts, and `sqlite3` CLI while the app runs; battle-tested
  crash safety (write-ahead log); file copy = backup; trivial restore; huge tooling
  ecosystem.
- **Cons**: new dependency; `AUTO_INCREMENT`/CLOB dialect differences in
  `schema.sql`; Java SQLite driver maturity slightly below H2's in-process story
  (fine at this scale).
- **Effort: ~1 day** including the 100%-coverage tests (adapter logic mostly
  unchanged).

### Option C — JSONL append-only file store (plain filesystem)

- **What**: new `FileConversationRepository` adapter. One directory per
  conversation under `data/conversations/{id}/`, containing:
  - `meta.json` — `{id, title, preset, createdAt}`,
  - `history.jsonl` — one JSON object per line, one per entry:
    `{"role":"user","content":"…","timestamp":"…"}`,
  - `{id}.log` (optional) — raw request/response event log.
  Load = read the file line by line; append = single `Files.write(..., APPEND)`
  with a file lock.
- **Why it fits Spring**: plain Java NIO, no new dependency; sits behind the
  existing port, so `ConversationService`, REST, and the UI are untouched.
- **Pros**: **human-readable** — `cat`, `grep`, `git diff` your chat history;
  append-only writes are inherently crash-safe (no partial-row risk beyond a torn
  last line, which is recoverable); conversations are self-contained folders —
  copy one folder = export one conversation; perfect for the "filesystem
  persistence" ask; trivially scriptable backups.
- **Cons**: no SQL/querying — `findAll()` scans directories and loads everything
  (fine for hundreds of conversations, degrades at thousands without an index
  file); you own pagination/search if ever needed; not a general-purpose data
  store for future features (events, task store).
- **Effort: ~1 day.** Risk: low, but it replaces SQL semantics — the BDD/unit
  tests must cover file locking and partial-line recovery.

## 6. Comparison matrix (the 3 best options for Spring)

| Criterion | A. H2 repaired | B. SQLite | C. JSONL files |
|---|---|---|---|
| Spring fit | native (status quo) | native (same JDBC stack) | NIO adapter behind port |
| New dependency | none | `sqlite-jdbc` | none |
| Single portable file | ✅ `mv.db` | ✅ `.sqlite` | ⚠️ one folder per conversation |
| Crash safety (kill mid-write) | ✅ MVStore + autocommit | ✅ WAL (strongest) | ✅ append-only; last line recoverable |
| Read/inspect while app runs | ⚠️ only with `AUTO_SERVER` | ✅ | ✅ |
| Human readability / grep | ❌ binary | ❌ binary (`sqlite3` CLI needed) | ✅ plain JSONL |
| Query/pagination/search later | ✅ SQL | ✅ SQL | ❌ DIY |
| Backup / export one conversation | DB-level only | DB-level only | ✅ copy a folder |
| Migration effort from today | minimal | ~1 day | ~1 day |
| 100%-coverage test impact | small | small | moderate (locking, recovery) |
| Scalability ceiling | high | high | low-medium (fine for this app) |

## 7. Recommendation

1. **Now — Option A (fix H2).** The symptom is not a storage-engine failure; it is
   the non-atomic turn in `ChatService` plus restart churn. Apply 4.1 (always
   persist the turn outcome) + 4.2 (single source of truth) + 4.3 (absolute path,
   `AUTO_SERVER`, index) and the 4.4 cleanup. This directly answers "how to fix
   that" and stops new dangling conversations today.
2. **If/when a heavier store is wanted — Option B (SQLite)** is the natural
   successor: same JDBC code, no lock wall, best crash story, still one file.
   Choose it when you want SQL queries over history or concurrent tooling.
3. **Choose Option C (JSONL) instead** only if human-readable, greppable,
   per-conversation-folder history is a first-class requirement (e.g. you want
   history visible in the repo/editor or shared as files).

All three keep the `ConversationRepository` port and the 100%-coverage gate intact;
A → B later is a drop-in adapter swap, so there is no lock-in either way.

## Appendix — adapter mapping

```
adapters.out.memory
├── JdbcConversationRepository      (today, H2)          → Option A (repaired)
│   └── + unified JdbcChatMemory over `messages`
├── SqliteConversationRepository    (Option B — same SQL, sqlite-jdbc DataSource)
└── FileConversationRepository      (Option C — data/conversations/{id}/meta.json + history.jsonl)
```

Port (unchanged): `core` → `ports/ConversationRepository` → `application/ConversationService` / `ChatService`.
