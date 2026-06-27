package com.legalease.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lawyer_id", referencedColumnName = "id")
    private Lawyer lawyer;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private String startTime; // e.g. "10:00 AM"

    @Column(name = "end_time", nullable = false)
    private String endTime; // e.g. "11:00 AM"

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING | APPROVED | CANCELLED | COMPLETED

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "UNPAID"; // UNPAID | PAID

    @Column(name = "complexity_rating")
    private String complexityRating; // LOW | MEDIUM | HIGH

    @Column(name = "complexity_report", columnDefinition = "TEXT")
    private String complexityReport; // Detail summary from Gemini complexity pre-screening

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Booking() {}

    public Booking(UUID id, User user, Lawyer lawyer, LocalDate bookingDate, String startTime, String endTime, String status, String paymentStatus, String complexityRating, String complexityReport, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.lawyer = lawyer;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.complexityRating = complexityRating;
        this.complexityReport = complexityReport;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = "UNPAID";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Lawyer getLawyer() { return lawyer; }
    public void setLawyer(Lawyer lawyer) { this.lawyer = lawyer; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getComplexityRating() { return complexityRating; }
    public void setComplexityRating(String complexityRating) { this.complexityRating = complexityRating; }

    public String getComplexityReport() { return complexityReport; }
    public void setComplexityReport(String complexityReport) { this.complexityReport = complexityReport; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private User user;
        private Lawyer lawyer;
        private LocalDate bookingDate;
        private String startTime;
        private String endTime;
        private String status = "PENDING";
        private String paymentStatus = "UNPAID";
        private String complexityRating;
        private String complexityReport;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder lawyer(Lawyer lawyer) { this.lawyer = lawyer; return this; }
        public Builder bookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder endTime(String endTime) { this.endTime = endTime; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder paymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public Builder complexityRating(String complexityRating) { this.complexityRating = complexityRating; return this; }
        public Builder complexityReport(String complexityReport) { this.complexityReport = complexityReport; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Booking build() {
            return new Booking(id, user, lawyer, bookingDate, startTime, endTime, status, paymentStatus, complexityRating, complexityReport, createdAt);
        }
    }
}
