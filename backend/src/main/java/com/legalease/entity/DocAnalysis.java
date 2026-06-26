package com.legalease.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doc_analyses")
public class DocAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false, unique = true)
    private Document document;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "risk_level")
    private String riskLevel; // LOW | MEDIUM | HIGH

    @Column(name = "key_clauses", columnDefinition = "TEXT")
    private String keyClauses; // JSON string of key clauses array

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse; // Direct dump from LLM

    @Column(name = "compliance_report", columnDefinition = "TEXT")
    private String complianceReport; // JSON string of compliance checks array

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DocAnalysis() {}

    public DocAnalysis(UUID id, Document document, String summary, String riskLevel, String keyClauses, String rawAiResponse, String complianceReport, LocalDateTime createdAt) {
        this.id = id;
        this.document = document;
        this.summary = summary;
        this.riskLevel = riskLevel;
        this.keyClauses = keyClauses;
        this.rawAiResponse = rawAiResponse;
        this.complianceReport = complianceReport;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getKeyClauses() { return keyClauses; }
    public void setKeyClauses(String keyClauses) { this.keyClauses = keyClauses; }

    public String getRawAiResponse() { return rawAiResponse; }
    public void setRawAiResponse(String rawAiResponse) { this.rawAiResponse = rawAiResponse; }

    public String getComplianceReport() { return complianceReport; }
    public void setComplianceReport(String complianceReport) { this.complianceReport = complianceReport; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Document document;
        private String summary;
        private String riskLevel;
        private String keyClauses;
        private String rawAiResponse;
        private String complianceReport;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder document(Document document) { this.document = document; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public Builder keyClauses(String keyClauses) { this.keyClauses = keyClauses; return this; }
        public Builder rawAiResponse(String rawAiResponse) { this.rawAiResponse = rawAiResponse; return this; }
        public Builder complianceReport(String complianceReport) { this.complianceReport = complianceReport; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DocAnalysis build() {
            return new DocAnalysis(id, document, summary, riskLevel, keyClauses, rawAiResponse, complianceReport, createdAt);
        }
    }
}
