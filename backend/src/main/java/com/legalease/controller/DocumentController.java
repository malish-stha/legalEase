package com.legalease.controller;

import com.legalease.dto.DocumentResponse;
import com.legalease.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadAndAnalyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "email", required = false) String paramEmail,
            @RequestParam(value = "name", required = false) String paramName,
            @RequestParam(value = "organizationId", required = false) UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        String clerkUserId = jwt.getSubject();
        
        // Extract email and name from JWT claims if available, else fallback to params
        String email = jwt.getClaim("email");
        if (email == null) {
            email = paramEmail;
        }
        
        String name = jwt.getClaim("name");
        if (name == null) {
            name = paramName;
        }

        log.info("Received upload request from Clerk User: {}, Email: {}, OrgId: {}", clerkUserId, email, organizationId);
        DocumentResponse response = documentService.uploadAndAnalyze(file, clerkUserId, email, name, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(@AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Listing documents for Clerk User: {}", clerkUserId);
        List<DocumentResponse> response = documentService.getUserDocuments(clerkUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentDetails(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Fetching details of document: {} for user: {}", id, clerkUserId);
        DocumentResponse response = documentService.getDocumentDetails(id, clerkUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Request to delete document: {} for user: {}", id, clerkUserId);
        documentService.deleteDocument(id, clerkUserId);
        return ResponseEntity.noContent().build();
    }
}
