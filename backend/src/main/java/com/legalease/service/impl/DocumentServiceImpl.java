package com.legalease.service.impl;

import com.legalease.dto.DocAnalysisResponse;
import com.legalease.dto.DocumentResponse;
import com.legalease.entity.DocAnalysis;
import com.legalease.entity.Document;
import com.legalease.entity.User;
import com.legalease.repository.DocAnalysisRepository;
import com.legalease.repository.DocEmbeddingRepository;
import com.legalease.repository.DocumentRepository;
import com.legalease.repository.UserRepository;
import com.legalease.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocAnalysisRepository docAnalysisRepository;
    private final DocEmbeddingRepository docEmbeddingRepository;
    
    private final StorageService storageService;
    private final DocumentParserService documentParserService;
    private final RedactionService redactionService;
    private final AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentServiceImpl(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocAnalysisRepository docAnalysisRepository,
            DocEmbeddingRepository docEmbeddingRepository,
            StorageService storageService,
            DocumentParserService documentParserService,
            RedactionService redactionService,
            AiService aiService) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.docAnalysisRepository = docAnalysisRepository;
        this.docEmbeddingRepository = docEmbeddingRepository;
        this.storageService = storageService;
        this.documentParserService = documentParserService;
        this.redactionService = redactionService;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public DocumentResponse uploadAndAnalyze(MultipartFile file, String clerkUserId, String userEmail, String userName) throws IOException {
        log.info("Processing document upload request. User: {}, File: {}, Content-Type: {}", 
                clerkUserId, file.getOriginalFilename(), file.getContentType());

        // 1. Sync User if not already present in db
        User user = userRepository.findById(clerkUserId)
                .orElseGet(() -> {
                    log.info("Creating new user profile for Clerk ID: {}", clerkUserId);
                    User newUser = User.builder()
                            .id(clerkUserId)
                            .email(userEmail != null ? userEmail : "no-email@legalease.com")
                            .name(userName != null ? userName : "LegalEase User")
                            .role("USER")
                            .build();
                    return userRepository.save(newUser);
                });

        // 2. Parse text from file. Support image OCR using Gemini Vision, or PDF/DOCX using Tika
        String rawText;
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            log.info("Image upload detected. Triggering Gemini Vision OCR...");
            rawText = aiService.performOcr(file.getBytes(), contentType);
        } else {
            rawText = documentParserService.parseDocument(file);
        }

        // 3. Redact PII (Anonymization)
        String redactedText = redactionService.redactPii(rawText);

        // 4. Upload original file to Supabase Storage
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + (fileExtension.isEmpty() ? "" : "." + fileExtension);
        String publicFileUrl = storageService.uploadFile(file, uniqueFileName);

        // 5. Create initial document record with status PROCESSING
        Document document = Document.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .fileUrl(publicFileUrl)
                .status("PROCESSING")
                .language("en")
                .build();
        document = documentRepository.save(document);

        // 6. Generate legal summary and chunk/embed document text
        try {
            String summaryJson = aiService.generateSummary(redactedText);
            
            // Parse fields from generated JSON response
            String title = document.getFileName();
            String summaryText = "";
            String riskLevel = "LOW";
            String keyClausesJson = "[]";

            try {
                JsonNode rootNode = objectMapper.readTree(summaryJson);
                if (rootNode.has("title")) {
                    title = rootNode.get("title").asText();
                }
                if (rootNode.has("summary")) {
                    summaryText = rootNode.get("summary").asText();
                }
                if (rootNode.has("riskLevel")) {
                    riskLevel = rootNode.get("riskLevel").asText();
                }
                if (rootNode.has("keyClauses")) {
                    keyClausesJson = objectMapper.writeValueAsString(rootNode.get("keyClauses"));
                }
            } catch (Exception parseException) {
                log.warn("Failed to parse JSON structured fields from Gemini response. Storing as fallback.", parseException);
                summaryText = summaryJson;
            }

            // Save Analysis details
            DocAnalysis docAnalysis = DocAnalysis.builder()
                    .document(document)
                    .summary(summaryText)
                    .riskLevel(riskLevel)
                    .keyClauses(keyClausesJson)
                    .rawAiResponse(summaryJson)
                    .build();
            docAnalysisRepository.save(docAnalysis);

            // Calculate chunk embeddings and save to vector store
            log.info("Triggering text chunking and vector embeddings generation for document ID: {}", document.getId());
            chunkAndEmbed(document, redactedText);

            // Update document title if extracted from AI analysis
            if (!title.isEmpty()) {
                document.setFileName(title);
            }
            document.setStatus("DONE");
            document = documentRepository.save(document);

            return mapToResponse(document, docAnalysis);

        } catch (Exception e) {
            log.error("AI Analysis/Embedding failed for document: {}", document.getId(), e);
            document.setStatus("FAILED");
            documentRepository.save(document);
            throw new IOException("Failed to analyze and embed document with AI", e);
        }
    }

    private void chunkAndEmbed(Document doc, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // Clean existing embeddings just in case of reprocessing
        docEmbeddingRepository.deleteByDocumentId(doc.getId());

        int chunkSize = 1000;
        int overlap = 200;
        int start = 0;
        int chunkIndex = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunkText = text.substring(start, end);

            try {
                float[] vector = aiService.embedText(chunkText);
                String vectorStr = Arrays.toString(vector);
                docEmbeddingRepository.insertEmbedding(doc.getId(), chunkText, chunkIndex, vectorStr);
            } catch (Exception embeddingEx) {
                log.error("Failed to generate/save embedding for chunk {} of document {}", chunkIndex, doc.getId(), embeddingEx);
            }

            if (end == text.length()) {
                break;
            }
            start += (chunkSize - overlap);
            chunkIndex++;
        }
        log.info("Successfully completed chunking and embedding. Saved {} chunks.", chunkIndex);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getUserDocuments(String clerkUserId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(clerkUserId).stream()
                .map(doc -> {
                    DocAnalysis analysis = docAnalysisRepository.findByDocumentId(doc.getId()).orElse(null);
                    return mapToResponse(doc, analysis);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentDetails(UUID documentId, String clerkUserId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        if (!document.getUser().getId().equals(clerkUserId)) {
            throw new SecurityException("User is not authorized to access this document");
        }

        DocAnalysis analysis = docAnalysisRepository.findByDocumentId(documentId).orElse(null);
        return mapToResponse(document, analysis);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastIndex = fileName.lastIndexOf('.');
        return lastIndex == -1 ? "" : fileName.substring(lastIndex + 1);
    }

    private DocumentResponse mapToResponse(Document doc, DocAnalysis analysis) {
        DocAnalysisResponse analysisResponse = null;
        if (analysis != null) {
            analysisResponse = DocAnalysisResponse.builder()
                    .id(analysis.getId())
                    .summary(analysis.getSummary())
                    .riskLevel(analysis.getRiskLevel())
                    .keyClauses(analysis.getKeyClauses())
                    .complianceReport(analysis.getComplianceReport())
                    .build();
        }

        return DocumentResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .fileUrl(doc.getFileUrl())
                .status(doc.getStatus())
                .language(doc.getLanguage())
                .createdAt(doc.getCreatedAt())
                .analysis(analysisResponse)
                .build();
    }
}
