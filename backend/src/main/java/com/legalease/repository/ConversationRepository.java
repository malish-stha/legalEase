package com.legalease.repository;

import com.legalease.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByUserIdAndDocumentId(String userId, UUID documentId);
    void deleteByDocumentId(UUID documentId);
}
