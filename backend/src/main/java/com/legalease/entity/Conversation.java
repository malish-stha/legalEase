package com.legalease.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "doc_id"})
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    private Document document;

    @Column(columnDefinition = "TEXT")
    private String messages; // JSON list string: [{"role":"user"|"assistant", "content": "..."}]

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Conversation() {}

    public Conversation(UUID id, User user, Document document, String messages, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.document = document;
        this.messages = messages;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.messages == null) {
            this.messages = "[]";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getMessages() { return messages; }
    public void setMessages(String messages) { this.messages = messages; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private User user;
        private Document document;
        private String messages;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder document(Document document) { this.document = document; return this; }
        public Builder messages(String messages) { this.messages = messages; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Conversation build() {
            return new Conversation(id, user, document, messages, createdAt);
        }
    }
}
