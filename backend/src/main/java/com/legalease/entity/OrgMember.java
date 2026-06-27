package com.legalease.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "org_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "organization_id"})
})
public class OrgMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String role; // ADMIN | MEMBER

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public OrgMember() {}

    public OrgMember(UUID id, User user, Organization organization, String role, LocalDateTime joinedAt) {
        this.id = id;
        this.user = user;
        this.organization = organization;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = "MEMBER";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private User user;
        private Organization organization;
        private String role;
        private LocalDateTime joinedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder organization(Organization organization) { this.organization = organization; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder joinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; return this; }

        public OrgMember build() {
            return new OrgMember(id, user, organization, role, joinedAt);
        }
    }
}
