package com.legalease.repository;

import com.legalease.entity.DocAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocAnalysisRepository extends JpaRepository<DocAnalysis, UUID> {
    Optional<DocAnalysis> findByDocumentId(UUID documentId);
}
