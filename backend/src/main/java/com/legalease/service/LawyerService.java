package com.legalease.service;

import com.legalease.entity.Lawyer;
import java.util.List;
import java.util.UUID;

public interface LawyerService {
    List<Lawyer> getAllLawyers();
    List<Lawyer> searchLawyers(String specialization, String location);
    Lawyer getLawyerById(UUID id);
}
