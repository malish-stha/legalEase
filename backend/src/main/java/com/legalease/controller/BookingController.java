package com.legalease.controller;

import com.legalease.entity.Booking;
import com.legalease.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestBody BookingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String clerkUserId = jwt.getSubject();
        log.info("Request to create booking for lawyer ID: {} from user: {}", request.getLawyerId(), clerkUserId);
        
        Booking booking = bookingService.createBooking(
                clerkUserId,
                request.getLawyerId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getUserExplanation(),
                request.getAssociatedDocId()
        );
        return ResponseEntity.ok(booking);
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getUserBookings(@AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Listing bookings for user: {}", clerkUserId);
        List<Booking> bookings = bookingService.getUserBookings(clerkUserId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Fetching details for booking ID: {} for user: {}", id, clerkUserId);
        Booking booking = bookingService.getBookingById(id, clerkUserId);
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateBookingStatus(
            @PathVariable("id") UUID id,
            @RequestParam("status") String status) {
        log.info("Updating status of booking ID: {} to: {}", id, status);
        Booking booking = bookingService.updateBookingStatus(id, status);
        return ResponseEntity.ok(booking);
    }

    public static class BookingRequest {
        private UUID lawyerId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate bookingDate;
        private String startTime;
        private String endTime;
        private String userExplanation;
        private UUID associatedDocId;

        public BookingRequest() {}

        public UUID getLawyerId() { return lawyerId; }
        public void setLawyerId(UUID lawyerId) { this.lawyerId = lawyerId; }

        public LocalDate getBookingDate() { return bookingDate; }
        public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }

        public String getUserExplanation() { return userExplanation; }
        public void setUserExplanation(String userExplanation) { this.userExplanation = userExplanation; }

        public UUID getAssociatedDocId() { return associatedDocId; }
        public void setAssociatedDocId(UUID associatedDocId) { this.associatedDocId = associatedDocId; }
    }
}
