package com.legalease.service;

import com.legalease.dto.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface DocumentService {
    /**
     * Uploads, parses, redacts, uploads to storage, analyzes via Gemini, and stores a document.
     * @param file User uploaded multipart file (PDF/DOCX).
     * @param clerkUserId Clerk user identifier.
     * @param userEmail Clerk email address.
     * @param userName Clerk user full name.
     * @return DocumentResponse mapped details.
     */
    DocumentResponse uploadAndAnalyze(MultipartFile file, String clerkUserId, String userEmail, String userName) throws IOException;

    /**
     * Lists all documents belonging to a user.
     * @param clerkUserId Clerk user identifier.
     * @return List of DocumentResponse.
     */
    List<DocumentResponse> getUserDocuments(String clerkUserId);

    /**
     * Gets a single document by ID.
     * @param documentId Document identifier.
     * @param clerkUserId Clerk user identifier for ownership check.
     * @return DocumentResponse details.
     */
    DocumentResponse getDocumentDetails(UUID documentId, String clerkUserId);
}
