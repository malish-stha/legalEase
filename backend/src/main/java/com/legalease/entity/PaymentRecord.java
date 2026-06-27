package com.legalease.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_records")
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "booking_id", referencedColumnName = "id")
    private Booking booking; // Optional, can be null if subscribing to personal/professional tier

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String gateway; // ESEWA | KHALTI | STRIPE

    @Column(name = "transaction_id")
    private String transactionId; // Gateway confirmation transaction reference

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING | SUCCESS | FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PaymentRecord() {}

    public PaymentRecord(UUID id, User user, Booking booking, Double amount, String gateway, String transactionId, String status, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.booking = booking;
        this.amount = amount;
        this.gateway = gateway;
        this.transactionId = transactionId;
        this.status = status;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private User user;
        private Booking booking;
        private Double amount;
        private String gateway;
        private String transactionId;
        private String status = "PENDING";
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder booking(Booking booking) { this.booking = booking; return this; }
        public Builder amount(Double amount) { this.amount = amount; return this; }
        public Builder gateway(String gateway) { this.gateway = gateway; return this; }
        public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public PaymentRecord build() {
            return new PaymentRecord(id, user, booking, amount, gateway, transactionId, status, createdAt);
        }
    }
}
