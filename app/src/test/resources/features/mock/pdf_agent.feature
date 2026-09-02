Feature: PDF extraction agent (mock LLM)

  Scenario: Extract data from a PDF document
    Given a pdf with text "Invoice #42 Total 100 USD"
    And the mock LLM will respond with "Extracted: Invoice #42, Total 100 USD"
    When the pdf agent receives instruction "Extract invoice data"
    Then the result contains "Extracted: Invoice #42"
    And the result data contains pages "1"

  Scenario: PDF agent refuses requests when stopped
    Given a pdf with text "Invoice #42 Total 100 USD"
    And the pdf agent is stopped
    When the pdf agent receives instruction "Extract invoice data"
    Then the request fails with agent stopped

  Scenario: PDF agent fails without a pdf payload
    When the pdf agent receives instruction "Extract invoice data" without a pdf
    Then the request fails with agent execution error
