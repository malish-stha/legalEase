package com.legalease.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DocumentResponse {
    private UUID id;
    private String fileName;
    private String fileUrl;
    private String status;
    private String language;
    private LocalDateTime createdAt;
    private DocAnalysisResponse analysis;

    public DocumentResponse() {}

    public DocumentResponse(UUID id, String fileName, String fileUrl, String status, String language, LocalDateTime createdAt, DocAnalysisResponse analysis) {
        this.id = id;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.status = status;
        this.language = language;
        this.createdAt = createdAt;
        this.analysis = analysis;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public DocAnalysisResponse getAnalysis() { return analysis; }
    public void setAnalysis(DocAnalysisResponse analysis) { this.analysis = analysis; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String fileName;
        private String fileUrl;
        private String status;
        private String language;
        private LocalDateTime createdAt;
        private DocAnalysisResponse analysis;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder analysis(DocAnalysisResponse analysis) { this.analysis = analysis; return this; }

        public DocumentResponse build() {
            return new DocumentResponse(id, fileName, fileUrl, status, language, createdAt, analysis);
        }
    }
}
