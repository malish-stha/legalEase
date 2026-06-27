package com.legalease.controller;

import com.legalease.entity.PaymentRecord;
import com.legalease.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/esewa/initiate")
    public ResponseEntity<Map<String, String>> initiateEsewa(
            @RequestParam(value = "bookingId", required = false) UUID bookingId,
            @RequestParam("amount") Double amount,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Initiating eSewa payment for user: {}, amount: {}", clerkUserId, amount);
        Map<String, String> result = paymentService.initiateEsewaPayment(clerkUserId, bookingId, amount);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/esewa/verify")
    public ResponseEntity<PaymentRecord> verifyEsewa(
            @RequestBody Map<String, String> payload) {
        log.info("Verifying eSewa payment callback...");
        PaymentRecord record = paymentService.verifyEsewaPayment(payload);
        return ResponseEntity.ok(record);
    }

    @PostMapping("/khalti/initiate")
    public ResponseEntity<Map<String, String>> initiateKhalti(
            @RequestParam(value = "bookingId", required = false) UUID bookingId,
            @RequestParam("amount") Double amount,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Initiating Khalti payment for user: {}, amount: {}", clerkUserId, amount);
        Map<String, String> result = paymentService.initiateKhaltiPayment(clerkUserId, bookingId, amount);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/khalti/verify")
    public ResponseEntity<PaymentRecord> verifyKhalti(
            @RequestBody Map<String, String> payload) {
        String pidx = payload.get("pidx");
        log.info("Verifying Khalti payment callback for pidx: {}", pidx);
        PaymentRecord record = paymentService.verifyKhaltiPayment(pidx);
        return ResponseEntity.ok(record);
    }
}
