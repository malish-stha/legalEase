package com.legalease.repository;

import com.legalease.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {
    List<PaymentRecord> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<PaymentRecord> findByTransactionId(String transactionId);
}
