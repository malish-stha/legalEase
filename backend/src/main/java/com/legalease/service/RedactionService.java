package com.legalease.service;

public interface RedactionService {
    /**
     * Redacts PII (Emails, Phone numbers, Citizenship IDs, etc.) from the given text.
     * @param text Original document text.
     * @return Redacted text safe for AI processing.
     */
    String redactPii(String text);
}
