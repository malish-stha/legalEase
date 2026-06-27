package com.legalease.service.impl;

import com.legalease.entity.Lawyer;
import com.legalease.repository.LawyerRepository;
import com.legalease.service.LawyerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LawyerServiceImpl implements LawyerService {

    private static final Logger log = LoggerFactory.getLogger(LawyerServiceImpl.class);

    private final LawyerRepository lawyerRepository;

    public LawyerServiceImpl(LawyerRepository lawyerRepository) {
        this.lawyerRepository = lawyerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lawyer> getAllLawyers() {
        log.info("Fetching all lawyers");
        return lawyerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lawyer> searchLawyers(String specialization, String location) {
        log.info("Searching lawyers with specialization: {}, location: {}", specialization, location);
        
        boolean hasSpec = specialization != null && !specialization.trim().isEmpty();
        boolean hasLoc = location != null && !location.trim().isEmpty();

        if (hasSpec && hasLoc) {
            return lawyerRepository.findBySpecializationIgnoreCaseAndLocationIgnoreCase(
                    specialization.trim(), location.trim());
        } else if (hasSpec) {
            return lawyerRepository.findBySpecializationIgnoreCase(specialization.trim());
        } else if (hasLoc) {
            return lawyerRepository.findByLocationIgnoreCase(location.trim());
        } else {
            return lawyerRepository.findAll();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Lawyer getLawyerById(UUID id) {
        log.info("Fetching lawyer with ID: {}", id);
        return lawyerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lawyer not found with ID: " + id));
    }
}
