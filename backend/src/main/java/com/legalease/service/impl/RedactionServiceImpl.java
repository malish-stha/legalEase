package com.legalease.service.impl;

import com.legalease.service.RedactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class RedactionServiceImpl implements RedactionService {

    private static final Logger log = LoggerFactory.getLogger(RedactionServiceImpl.class);

    // Regex Patterns for PII Redaction
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"
    );

    // Nepal & International phone formats (e.g. +977-98XXXXXXXX, 98XXXXXXXX, 01-XXXXXXX)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(?:\\+?977[- ]?)?(?:9[678]\\d{8}|01[- ]?\\d{7}|\\d{3}[- ]?\\d{3}[- ]?\\d{4})\\b"
    );

    // Citizenship / National ID numbers in Nepal (e.g. 27-01-72-03829, 271027/182, 123-456-789)
    private static final Pattern CITIZENSHIP_ID_PATTERN = Pattern.compile(
            "\\b\\d{1,4}[-/]\\d{1,4}[-/]\\d{1,6}(?:[-/]\\d{1,6})?\\b"
    );

    // General pattern matching for potentially sensitive document IDs (like Passport numbers)
    private static final Pattern PASSPORT_PATTERN = Pattern.compile(
            "\\b[A-Z]\\d{7,8}\\b"
    );

    @Override
    public String redactPii(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        log.debug("Initiating PII redaction on text length: {}", text.length());

        String redacted = text;

        // Redact email addresses
        redacted = EMAIL_PATTERN.matcher(redacted).replaceAll("[EMAIL_REDACTED]");

        // Redact phone numbers
        redacted = PHONE_PATTERN.matcher(redacted).replaceAll("[PHONE_REDACTED]");

        // Redact citizenship IDs
        redacted = CITIZENSHIP_ID_PATTERN.matcher(redacted).replaceAll("[ID_REDACTED]");

        // Redact passport numbers
        redacted = PASSPORT_PATTERN.matcher(redacted).replaceAll("[PASSPORT_REDACTED]");

        log.debug("Redaction complete. Post-redacted text length: {}", redacted.length());
        return redacted;
    }
}
