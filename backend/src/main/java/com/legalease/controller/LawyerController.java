package com.legalease.controller;

import com.legalease.entity.Lawyer;
import com.legalease.service.LawyerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lawyers")
public class LawyerController {

    private static final Logger log = LoggerFactory.getLogger(LawyerController.class);

    private final LawyerService lawyerService;

    public LawyerController(LawyerService lawyerService) {
        this.lawyerService = lawyerService;
    }

    @GetMapping
    public ResponseEntity<List<Lawyer>> searchLawyers(
            @RequestParam(value = "specialization", required = false) String specialization,
            @RequestParam(value = "location", required = false) String location) {
        log.info("Searching lawyers. Spec: {}, Loc: {}", specialization, location);
        List<Lawyer> result = lawyerService.searchLawyers(specialization, location);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lawyer> getLawyerById(@PathVariable("id") UUID id) {
        log.info("Fetching lawyer profile for ID: {}", id);
        Lawyer lawyer = lawyerService.getLawyerById(id);
        return ResponseEntity.ok(lawyer);
    }
}
