package com.legalease.controller;

import com.legalease.entity.DocEmbedding;
import com.legalease.entity.Document;
import com.legalease.repository.DocEmbeddingRepository;
import com.legalease.repository.DocumentRepository;
import com.legalease.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class ComparisonController {

    private static final Logger log = LoggerFactory.getLogger(ComparisonController.class);

    private final DocumentRepository documentRepository;
    private final DocEmbeddingRepository docEmbeddingRepository;
    private final AiService aiService;

    public ComparisonController(
            DocumentRepository documentRepository,
            DocEmbeddingRepository docEmbeddingRepository,
            AiService aiService) {
        this.documentRepository = documentRepository;
        this.docEmbeddingRepository = docEmbeddingRepository;
        this.aiService = aiService;
    }

    @PostMapping("/compare")
    public ResponseEntity<Map<String, String>> compareDocuments(
            @RequestBody CompareRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String clerkUserId = jwt.getSubject();
        UUID baseDocId = request.getBaseDocId();
        UUID compareDocId = request.getCompareDocId();

        log.info("Comparing base document ID: {} with modified document ID: {} for user: {}", 
                baseDocId, compareDocId, clerkUserId);

        if (baseDocId == null || compareDocId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Both baseDocId and compareDocId must be provided."));
        }

        // Load base document & verify ownership
        Document baseDoc = documentRepository.findById(baseDocId)
                .orElseThrow(() -> new IllegalArgumentException("Base document not found: " + baseDocId));
        if (!baseDoc.getUser().getId().equals(clerkUserId)) {
            throw new SecurityException("User is not authorized to access base document");
        }

        // Load compare document & verify ownership
        Document compareDoc = documentRepository.findById(compareDocId)
                .orElseThrow(() -> new IllegalArgumentException("Comparison document not found: " + compareDocId));
        if (!compareDoc.getUser().getId().equals(clerkUserId)) {
            throw new SecurityException("User is not authorized to access comparison document");
        }

        // Reassemble base text
        List<DocEmbedding> baseEmbeddings = docEmbeddingRepository.findByDocumentIdOrderByChunkIndexAsc(baseDocId);
        if (baseEmbeddings.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No text chunks found for base document ID: " + baseDocId));
        }
        String baseText = baseEmbeddings.stream()
                .map(DocEmbedding::getChunkText)
                .collect(Collectors.joining(" "));

        // Reassemble compare text
        List<DocEmbedding> compareEmbeddings = docEmbeddingRepository.findByDocumentIdOrderByChunkIndexAsc(compareDocId);
        if (compareEmbeddings.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No text chunks found for comparison document ID: " + compareDocId));
        }
        String compareText = compareEmbeddings.stream()
                .map(DocEmbedding::getChunkText)
                .collect(Collectors.joining(" "));

        // Call Gemini semantic diffing
        String comparisonReport = aiService.compareDocuments(baseText, compareText);

        log.info("Semantic document comparison completed successfully.");
        return ResponseEntity.ok(Map.of("comparisonReport", comparisonReport));
    }

    public static class CompareRequest {
        private UUID baseDocId;
        private UUID compareDocId;

        public CompareRequest() {}

        public UUID getBaseDocId() { return baseDocId; }
        public void setBaseDocId(UUID baseDocId) { this.baseDocId = baseDocId; }

        public UUID getCompareDocId() { return compareDocId; }
        public void setCompareDocId(UUID compareDocId) { this.compareDocId = compareDocId; }
    }
}
