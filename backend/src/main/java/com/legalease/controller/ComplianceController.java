package com.legalease.controller;

import com.legalease.entity.DocAnalysis;
import com.legalease.entity.DocEmbedding;
import com.legalease.entity.Document;
import com.legalease.repository.DocAnalysisRepository;
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
public class ComplianceController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceController.class);

    private final DocumentRepository documentRepository;
    private final DocAnalysisRepository docAnalysisRepository;
    private final DocEmbeddingRepository docEmbeddingRepository;
    private final AiService aiService;

    public ComplianceController(
            DocumentRepository documentRepository,
            DocAnalysisRepository docAnalysisRepository,
            DocEmbeddingRepository docEmbeddingRepository,
            AiService aiService) {
        this.documentRepository = documentRepository;
        this.docAnalysisRepository = docAnalysisRepository;
        this.docEmbeddingRepository = docEmbeddingRepository;
        this.aiService = aiService;
    }

    @GetMapping("/{id}/compliance")
    public ResponseEntity<Map<String, String>> getLaborCompliance(
            @PathVariable("id") UUID documentId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String clerkUserId = jwt.getSubject();
        log.info("Requesting Labor Law compliance for document ID: {}, User: {}", documentId, clerkUserId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        if (!document.getUser().getId().equals(clerkUserId)) {
            throw new SecurityException("User is not authorized to access this document");
        }

        DocAnalysis analysis = docAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalStateException("Analysis not found for document ID: " + documentId));

        // If compliance report is already cached in database, return it
        if (analysis.getComplianceReport() != null && !analysis.getComplianceReport().trim().isEmpty()) {
            log.info("Returning cached compliance report for document ID: {}", documentId);
            return ResponseEntity.ok(Map.of("complianceReport", analysis.getComplianceReport()));
        }

        // If not cached, reassemble document text from chunks
        log.info("Reassembling full text from chunks for compliance checks...");
        List<DocEmbedding> embeddings = docEmbeddingRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        if (embeddings.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No text chunks found for compliance analysis"));
        }

        String fullText = embeddings.stream()
                .map(DocEmbedding::getChunkText)
                .collect(Collectors.joining(" "));

        // Run compliance check with Gemini
        String reportJson = aiService.checkLaborCompliance(fullText);

        // Cache report in database
        analysis.setComplianceReport(reportJson);
        docAnalysisRepository.save(analysis);

        log.info("Compliance analysis generated and cached for document ID: {}", documentId);
        return ResponseEntity.ok(Map.of("complianceReport", reportJson));
    }
}
