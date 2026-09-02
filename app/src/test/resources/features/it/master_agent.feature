@it
Feature: Master agent against a real LLM

  Scenario: Master answers a question
    When the user sends "Reply with exactly: OK"
    Then the reply is not empty
