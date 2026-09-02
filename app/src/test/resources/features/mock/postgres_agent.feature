Feature: Postgres expert agent (mock LLM)

  Scenario: Postgres agent loads a reference topic before answering
    Given the mock LLM will call tool "loadReference" with arguments "{\"topic\":\"schema-design\"}" and then respond with "Use BIGINT GENERATED ALWAYS AS IDENTITY for primary keys."
    When the postgres agent receives instruction "How should I design primary keys?"
    Then the result contains "BIGINT GENERATED ALWAYS AS IDENTITY"
    And the mock LLM received a tool result containing "BIGINT GENERATED ALWAYS AS IDENTITY"

  Scenario: Postgres agent answers a question without loading references
    Given the mock LLM will respond with "VACUUM reclaims dead tuples and prevents XID wraparound."
    When the postgres agent receives instruction "What does VACUUM do?"
    Then the result contains "VACUUM"

  Scenario: Postgres agent refuses requests when stopped
    Given the postgres agent is stopped
    When the postgres agent receives instruction "Explain indexes"
    Then the request fails with agent stopped
