@it
Feature: PDF agent against a real LLM

  Scenario: PDF agent extracts a name from a document
    Given a pdf with text "Name: John Doe Amount: 500 USD"
    When the pdf agent receives instruction "Extract the name from the document"
    Then the result contains "John Doe"
