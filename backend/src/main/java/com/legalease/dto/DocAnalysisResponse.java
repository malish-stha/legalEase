package com.legalease.dto;

import java.util.UUID;

public class DocAnalysisResponse {
    private UUID id;
    private String summary;
    private String riskLevel;
    private String keyClauses; // JSON array string

    public DocAnalysisResponse() {}

    public DocAnalysisResponse(UUID id, String summary, String riskLevel, String keyClauses) {
        this.id = id;
        this.summary = summary;
        this.riskLevel = riskLevel;
        this.keyClauses = keyClauses;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getKeyClauses() { return keyClauses; }
    public void setKeyClauses(String keyClauses) { this.keyClauses = keyClauses; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String summary;
        private String riskLevel;
        private String keyClauses;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public Builder keyClauses(String keyClauses) { this.keyClauses = keyClauses; return this; }

        public DocAnalysisResponse build() {
            return new DocAnalysisResponse(id, summary, riskLevel, keyClauses);
        }
    }
}
