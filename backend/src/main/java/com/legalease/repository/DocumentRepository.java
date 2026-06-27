package com.legalease.repository;

import com.legalease.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Document> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
