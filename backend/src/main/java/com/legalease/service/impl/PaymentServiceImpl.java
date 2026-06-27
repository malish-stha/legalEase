package com.legalease.service.impl;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalease.entity.Booking;
import com.legalease.entity.PaymentRecord;
import com.legalease.entity.User;
import com.legalease.repository.BookingRepository;
import com.legalease.repository.PaymentRecordRepository;
import com.legalease.repository.UserRepository;
import com.legalease.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRecordRepository paymentRecordRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.esewa.secret-key:8gBm/:&EnhH.1/q}")
    private String esewaSecretKey;

    @Value("${app.esewa.product-code:EPAYTEST}")
    private String esewaProductCode;

    @Value("${app.khalti.secret-key:test_secret_key_8407cb5e8dc94132a0c64883f3e1b764}")
    private String khaltiSecretKey;

    public PaymentServiceImpl(
            PaymentRecordRepository paymentRecordRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Map<String, String> initiateEsewaPayment(String clerkUserId, UUID bookingId, Double amount) {
        String clientOrigin = getClientOrigin();
        log.info("Initiating eSewa payment. User: {}, Booking: {}, Amount: {}, Origin: {}", clerkUserId, bookingId, amount, clientOrigin);

        User user = getOrCreateUser(clerkUserId);

        Booking booking = null;
        if (bookingId != null) {
            booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        }

        // Create initial pending record
        String transactionUuid = UUID.randomUUID().toString();
        PaymentRecord record = PaymentRecord.builder()
                .user(user)
                .booking(booking)
                .amount(amount)
                .gateway("ESEWA")
                .transactionId(transactionUuid) // Use transactionUuid as initial reference
                .status("PENDING")
                .build();
        paymentRecordRepository.save(record);

        // Format amount: eSewa requires .00 format or matching exactly
        String formattedAmount = String.format("%.2f", amount);

        // Generate signature: total_amount=100.00,transaction_uuid=1234,product_code=EPAYTEST
        String signature = generateEsewaSignature(formattedAmount, transactionUuid, esewaProductCode, esewaSecretKey);

        String baseHost = (clientOrigin != null && !clientOrigin.isBlank()) ? clientOrigin : "http://localhost:3001";
        if (baseHost.endsWith("/")) {
            baseHost = baseHost.substring(0, baseHost.length() - 1);
        }

        Map<String, String> params = new HashMap<>();
        params.put("amount", formattedAmount);
        params.put("tax_amount", "0");
        params.put("product_service_charge", "0");
        params.put("product_delivery_charge", "0");
        params.put("product_code", esewaProductCode);
        params.put("total_amount", formattedAmount);
        params.put("transaction_uuid", transactionUuid);
        params.put("signature", signature);
        params.put("signed_field_names", "total_amount,transaction_uuid,product_code");
        params.put("success_url", baseHost + "/billing/success");
        params.put("failure_url", baseHost + "/billing/failure");

        log.info("Generated eSewa payment signature successfully.");
        return params;
    }

    @Override
    @Transactional
    public PaymentRecord verifyEsewaPayment(Map<String, String> params) {
        log.info("Verifying eSewa payment response...");
        String encodedData = params.get("data");
        if (encodedData == null) {
            throw new IllegalArgumentException("Invalid eSewa response: data parameter is missing.");
        }

        try {
            // Decode base64 response containing transaction JSON
            byte[] decodedBytes = Base64.getDecoder().decode(encodedData.trim());
            String decodedJson = new String(decodedBytes, StandardCharsets.UTF_8);
            log.info("Decoded eSewa JSON response: {}", decodedJson);

            JsonNode root = objectMapper.readTree(decodedJson);
            String status = root.path("status").asText();
            String totalAmount = root.path("total_amount").asText();
            String transactionUuid = root.path("transaction_uuid").asText();
            String productCode = root.path("product_code").asText();
            String signature = root.path("signature").asText();
            String transactionCode = root.path("transaction_code").asText();

            if (!"COMPLETE".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Payment status is not complete. Status: " + status);
            }

            // Verify signature
            String expectedSignature = generateEsewaSignature(totalAmount, transactionUuid, productCode, esewaSecretKey);
            if (!expectedSignature.equals(signature)) {
                log.error("Signature verification failed. Expected: {}, Received: {}", expectedSignature, signature);
                throw new SecurityException("Payment signature verification failed.");
            }

            // Find matching pending payment record
            PaymentRecord record = paymentRecordRepository.findByTransactionId(transactionUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction record not found: " + transactionUuid));

            record.setStatus("SUCCESS");
            record.setTransactionId(transactionCode); // Update with gateway transaction reference
            paymentRecordRepository.save(record);

            // Update Booking status to PAID/APPROVED if attached
            if (record.getBooking() != null) {
                Booking booking = record.getBooking();
                booking.setPaymentStatus("PAID");
                booking.setStatus("APPROVED");
                bookingRepository.save(booking);
            }

            log.info("eSewa transaction verified successfully. Ref: {}", transactionCode);
            return record;

        } catch (Exception e) {
            log.error("Failed to verify eSewa payment signature/response", e);
            throw new RuntimeException("eSewa verification failed", e);
        }
    }

    @Override
    @Transactional
    public Map<String, String> initiateKhaltiPayment(String clerkUserId, UUID bookingId, Double amount) {
        String clientOrigin = getClientOrigin();
        log.info("Initiating Khalti payment. User: {}, Booking: {}, Amount: {}, Origin: {}", clerkUserId, bookingId, amount, clientOrigin);

        User user = getOrCreateUser(clerkUserId);

        Booking booking = null;
        if (bookingId != null) {
            booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        }

        String transactionUuid = UUID.randomUUID().toString();
        PaymentRecord record = PaymentRecord.builder()
                .user(user)
                .booking(booking)
                .amount(amount)
                .gateway("KHALTI")
                .transactionId(transactionUuid) // Pre-assigned temp UUID
                .status("PENDING")
                .build();
        paymentRecordRepository.save(record);

        // Khalti expects amount in paisa (Rs. 1 = 100 paisa)
        long amountInPaisa = Math.round(amount * 100);

        String baseHost = (clientOrigin != null && !clientOrigin.isBlank()) ? clientOrigin : "http://localhost:3001";
        if (baseHost.endsWith("/")) {
            baseHost = baseHost.substring(0, baseHost.length() - 1);
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("return_url", baseHost + "/billing/success");
            payload.put("website_url", baseHost);
            payload.put("amount", amountInPaisa);
            payload.put("purchase_order_id", transactionUuid);
            payload.put("purchase_order_name", bookingId != null ? "Consultation Booking" : "LegalEase Subscription");

            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Call Khalti initiate sandbox API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://dev.khalti.com/api/v2/epayment/initiate/"))
                    .header("Authorization", "Key " + khaltiSecretKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Khalti initiate raw response status: {}", response.statusCode());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Khalti initiate failed: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String pidx = root.path("pidx").asText();
            String paymentUrl = root.path("payment_url").asText();

            // Update record transactionId with pidx for lookup matching later
            record.setTransactionId(pidx);
            paymentRecordRepository.save(record);

            log.info("Khalti payment session initiated. pidx: {}", pidx);
            return Map.of("pidx", pidx, "payment_url", paymentUrl);

        } catch (Exception e) {
            log.error("Failed to initiate Khalti payment", e);
            throw new RuntimeException("Khalti initiation failed", e);
        }
    }

    @Override
    @Transactional
    public PaymentRecord verifyKhaltiPayment(String pidx) {
        log.info("Verifying Khalti transaction with pidx: {}", pidx);

        try {
            Map<String, String> payload = Map.of("pidx", pidx);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://dev.khalti.com/api/v2/epayment/lookup/"))
                    .header("Authorization", "Key " + khaltiSecretKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Khalti lookup response status: {}", response.statusCode());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Khalti lookup failed: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText();
            String transactionId = root.path("transaction_id").asText();

            if (!"Completed".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Khalti transaction is not completed. Status: " + status);
            }

            PaymentRecord record = paymentRecordRepository.findByTransactionId(pidx)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction record not found for pidx: " + pidx));

            record.setStatus("SUCCESS");
            record.setTransactionId(transactionId); // Save actual Khalti reference ID
            paymentRecordRepository.save(record);

            if (record.getBooking() != null) {
                Booking booking = record.getBooking();
                booking.setPaymentStatus("PAID");
                booking.setStatus("APPROVED");
                bookingRepository.save(booking);
            }

            log.info("Khalti transaction completed successfully. Ref: {}", transactionId);
            return record;

        } catch (Exception e) {
            log.error("Failed to verify Khalti payment", e);
            throw new RuntimeException("Khalti verification failed", e);
        }
    }

    private String generateEsewaSignature(String totalAmount, String transactionUuid, String productCode, String secretKey) {
        try {
            String message = String.format("total_amount=%s,transaction_uuid=%s,product_code=%s", totalAmount, transactionUuid, productCode);
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC SHA256 signature for eSewa", e);
        }
    }

    private String getClientOrigin() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String origin = request.getHeader("Origin");
            if (origin == null || origin.isBlank()) {
                origin = request.getHeader("Referer");
            }
            if (origin != null && !origin.isBlank()) {
                try {
                    java.net.URI uri = new java.net.URI(origin);
                    return uri.getScheme() + "://" + uri.getAuthority();
                } catch (Exception e) {
                    return origin;
                }
            }
        }
        return "http://localhost:3001"; // Default fallback
    }

    private User getOrCreateUser(String clerkUserId) {
        return userRepository.findById(clerkUserId)
                .orElseGet(() -> {
                    String email = "user-" + clerkUserId + "@legalease.com";
                    String name = "LegalEase User";

                    org.springframework.security.core.Authentication auth = 
                            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
                        org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) auth.getPrincipal();
                        String jwtEmail = jwt.getClaim("email");
                        String jwtName = jwt.getClaim("name");
                        if (jwtEmail != null) email = jwtEmail;
                        if (jwtName != null) name = jwtName;
                    }

                    log.info("Creating new user profile for Clerk ID: {} via auto-sync, Email: {}, Name: {}", clerkUserId, email, name);
                    User newUser = User.builder()
                            .id(clerkUserId)
                            .email(email)
                            .name(name)
                            .role("USER")
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
