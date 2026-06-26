package com.legalease.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doc_embeddings")
public class DocEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    private Document document;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DocEmbedding() {}

    public DocEmbedding(UUID id, Document document, String chunkText, Integer chunkIndex, LocalDateTime createdAt) {
        this.id = id;
        this.document = document;
        this.chunkText = chunkText;
        this.chunkIndex = chunkIndex;
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

    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Document document;
        private String chunkText;
        private Integer chunkIndex;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder document(Document document) { this.document = document; return this; }
        public Builder chunkText(String chunkText) { this.chunkText = chunkText; return this; }
        public Builder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DocEmbedding build() {
            return new DocEmbedding(id, document, chunkText, chunkIndex, createdAt);
        }
    }
}
