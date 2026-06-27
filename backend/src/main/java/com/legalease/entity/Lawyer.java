package com.legalease.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lawyers")
public class Lawyer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String specialization; // e.g. CORPORATE, LABOUR, CIVIL, FAMILY, CRIMINAL

    @Column(nullable = false)
    private Double rating = 5.0;

    @Column(name = "hourly_rate", nullable = false)
    private Double hourlyRate;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false)
    private String location;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @Column(columnDefinition = "TEXT")
    private String availability; // JSON array of available time slots (e.g. ["2026-06-28 10:00", "2026-06-28 14:00"])

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Lawyer() {}

    public Lawyer(UUID id, String name, String email, String phone, String specialization, Double rating, Double hourlyRate, String bio, String location, Integer experienceYears, String availability, Boolean isVerified, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.specialization = specialization;
        this.rating = rating;
        this.hourlyRate = hourlyRate;
        this.bio = bio;
        this.location = location;
        this.experienceYears = experienceYears;
        this.availability = availability;
        this.isVerified = isVerified;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.rating == null) {
            this.rating = 5.0;
        }
        if (this.isVerified == null) {
            this.isVerified = true;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private String email;
        private String phone;
        private String specialization;
        private Double rating = 5.0;
        private Double hourlyRate;
        private String bio;
        private String location;
        private Integer experienceYears;
        private String availability;
        private Boolean isVerified = true;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder specialization(String specialization) { this.specialization = specialization; return this; }
        public Builder rating(Double rating) { this.rating = rating; return this; }
        public Builder hourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; return this; }
        public Builder bio(String bio) { this.bio = bio; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder experienceYears(Integer experienceYears) { this.experienceYears = experienceYears; return this; }
        public Builder availability(String availability) { this.availability = availability; return this; }
        public Builder isVerified(Boolean isVerified) { this.isVerified = isVerified; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Lawyer build() {
            return new Lawyer(id, name, email, phone, specialization, rating, hourlyRate, bio, location, experienceYears, availability, isVerified, createdAt);
        }
    }
}
