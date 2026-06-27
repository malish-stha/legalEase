package com.legalease.repository;

import com.legalease.entity.Lawyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LawyerRepository extends JpaRepository<Lawyer, UUID> {
    List<Lawyer> findBySpecializationIgnoreCase(String specialization);
    List<Lawyer> findByLocationIgnoreCase(String location);
    List<Lawyer> findBySpecializationIgnoreCaseAndLocationIgnoreCase(String specialization, String location);
}
