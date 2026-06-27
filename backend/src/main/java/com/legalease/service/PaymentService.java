package com.legalease.service;

import com.legalease.entity.PaymentRecord;
import java.util.Map;
import java.util.UUID;

public interface PaymentService {
    Map<String, String> initiateEsewaPayment(String clerkUserId, UUID bookingId, Double amount);
    PaymentRecord verifyEsewaPayment(Map<String, String> params);
    Map<String, String> initiateKhaltiPayment(String clerkUserId, UUID bookingId, Double amount);
    PaymentRecord verifyKhaltiPayment(String pidx);
}
