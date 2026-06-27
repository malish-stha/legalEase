package com.legalease.controller;

import com.legalease.entity.DocAnalysis;
import com.legalease.entity.Document;
import com.legalease.repository.DocAnalysisRepository;
import com.legalease.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class HealthScoreController {

    private static final Logger log = LoggerFactory.getLogger(HealthScoreController.class);

    private final DocumentRepository documentRepository;
    private final DocAnalysisRepository docAnalysisRepository;

    public HealthScoreController(
            DocumentRepository documentRepository,
            DocAnalysisRepository docAnalysisRepository) {
        this.documentRepository = documentRepository;
        this.docAnalysisRepository = docAnalysisRepository;
    }

    @GetMapping("/health-score")
    public ResponseEntity<Map<String, Object>> getGlobalHealthScore(@AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Calculating Global Legal Health Score for user: {}", clerkUserId);

        List<Document> documents = documentRepository.findByUserIdOrderByCreatedAtDesc(clerkUserId);
        
        int totalDocuments = documents.size();
        int lowRisk = 0;
        int mediumRisk = 0;
        int highRisk = 0;
        int score = 100;

        for (Document doc : documents) {
            DocAnalysis analysis = docAnalysisRepository.findByDocumentId(doc.getId()).orElse(null);
            if (analysis != null) {
                String risk = analysis.getRiskLevel();
                if ("HIGH".equalsIgnoreCase(risk)) {
                    highRisk++;
                    score -= 25;
                } else if ("MEDIUM".equalsIgnoreCase(risk)) {
                    mediumRisk++;
                    score -= 10;
                } else {
                    lowRisk++;
                }
            } else {
                lowRisk++; // Default to low/safe if not analyzed yet
            }
        }

        // Clamp the score between 10 and 100
        score = Math.max(10, Math.min(100, score));

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("totalDocuments", totalDocuments);
        result.put("lowRiskCount", lowRisk);
        result.put("mediumRiskCount", mediumRisk);
        result.put("highRiskCount", highRisk);

        log.info("Global Health Score calculated: {}", score);
        return ResponseEntity.ok(result);
    }
}
