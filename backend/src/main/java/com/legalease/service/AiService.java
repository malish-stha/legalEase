package com.legalease.service;

public interface AiService {
    /**
     * Sends document text to Gemini and gets a structured JSON summary.
     * @param redactedText Text content of the document (PII redacted).
     * @return JSON string of document summary and risk analysis.
     */
    String generateSummary(String redactedText);
}
