package com.legalease.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalease.entity.Booking;
import com.legalease.entity.DocEmbedding;
import com.legalease.entity.Lawyer;
import com.legalease.entity.User;
import com.legalease.repository.BookingRepository;
import com.legalease.repository.DocEmbeddingRepository;
import com.legalease.repository.LawyerRepository;
import com.legalease.repository.UserRepository;
import com.legalease.service.AiService;
import com.legalease.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final LawyerRepository lawyerRepository;
    private final DocEmbeddingRepository docEmbeddingRepository;
    private final AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            LawyerRepository lawyerRepository,
            DocEmbeddingRepository docEmbeddingRepository,
            AiService aiService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.lawyerRepository = lawyerRepository;
        this.docEmbeddingRepository = docEmbeddingRepository;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public Booking createBooking(
            String clerkUserId,
            UUID lawyerId,
            LocalDate date,
            String startTime,
            String endTime,
            String userExplanation,
            UUID associatedDocId) {
        
        log.info("Creating appointment booking for Clerk User: {}, Lawyer ID: {}", clerkUserId, lawyerId);

        User user = getOrCreateUser(clerkUserId);

        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new IllegalArgumentException("Lawyer not found with ID: " + lawyerId));

        // Reassemble document text if associatedDocId is present
        String docText = null;
        if (associatedDocId != null) {
            log.info("Loading linked document ID: {} for AI pre-screening context", associatedDocId);
            List<DocEmbedding> embeddings = docEmbeddingRepository.findByDocumentIdOrderByChunkIndexAsc(associatedDocId);
            if (!embeddings.isEmpty()) {
                docText = embeddings.stream()
                        .map(DocEmbedding::getChunkText)
                        .collect(Collectors.joining(" "));
            }
        }

        // Run Gemini AI pre-screening assessment
        String rating = "LOW";
        String report = "AI Pre-screening not available.";
        
        try {
            log.info("Triggering Gemini AI pre-screening assessment...");
            String aiResponse = aiService.preScreenCase(userExplanation, docText);
            
            JsonNode root = objectMapper.readTree(aiResponse);
            if (root.has("complexityRating")) {
                rating = root.get("complexityRating").asText();
            }
            if (root.has("report")) {
                report = root.get("report").asText();
            } else {
                report = aiResponse;
            }
        } catch (Exception e) {
            log.warn("Gemini AI pre-screening assessment failed. Proceeding with fallback details.", e);
            report = "Pre-screening failed. Issue Description: " + userExplanation;
        }

        Booking booking = Booking.builder()
                .user(user)
                .lawyer(lawyer)
                .bookingDate(date)
                .startTime(startTime)
                .endTime(endTime)
                .status("PENDING")
                .paymentStatus("UNPAID")
                .complexityRating(rating)
                .complexityReport(report)
                .build();

        log.info("Booking successfully created with complexity rating: {}", rating);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(String clerkUserId) {
        log.info("Fetching all bookings for Clerk User: {}", clerkUserId);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(clerkUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getLawyerBookings(UUID lawyerId) {
        log.info("Fetching all bookings for Lawyer ID: {}", lawyerId);
        return bookingRepository.findByLawyerIdOrderByBookingDateDesc(lawyerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(UUID id, String clerkUserId) {
        log.info("Fetching details of booking: {} for user: {}", id, clerkUserId);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));

        if (!booking.getUser().getId().equals(clerkUserId)) {
            throw new SecurityException("User is not authorized to access this booking details");
        }

        return booking;
    }

    @Override
    @Transactional
    public Booking updateBookingStatus(UUID id, String status) {
        log.info("Updating booking ID: {} status to: {}", id, status);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));
        booking.setStatus(status);
        return bookingRepository.save(booking);
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
