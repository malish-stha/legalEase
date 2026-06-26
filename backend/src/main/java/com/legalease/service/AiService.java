package com.legalease.service;

public interface AiService {
    /**
     * Sends document text to Gemini and gets a structured JSON summary.
     * @param redactedText Text content of the document (PII redacted).
     * @return JSON string of document summary and risk analysis.
     */
    String generateSummary(String redactedText);

    /**
     * Computes text vector embeddings using Gemini text-embedding-004.
     * @param text Chunk text.
     * @return 768-dimensional float vector.
     */
    float[] embedText(String text);

    /**
     * Extracts text from document images using Gemini 2.0 Flash Vision.
     * @param imageBytes Binary image payload.
     * @param mimeType Image mime type (image/png, image/jpeg, etc.)
     * @return Extracted plain text.
     */
    String performOcr(byte[] imageBytes, String mimeType);

    /**
     * Streams responses for RAG chat.
     */
    void streamChatResponse(String systemPrompt, String userPrompt, dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage> handler);

    /**
     * Runs Nepal Labor Law compliance check on document text.
     * @param documentText Full text content of the contract.
     * @return JSON string of compliance checks.
     */
    String checkLaborCompliance(String documentText);

    /**
     * Semantically compares two legal documents.
     * @param baseText Full text of the base document.
     * @param compareText Full text of the modified document.
     * @return JSON analysis of the changes and comparison details.
     */
    String compareDocuments(String baseText, String compareText);
}
